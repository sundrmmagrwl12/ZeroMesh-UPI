package com.zeromesh.service.ingestion;

import com.zeromesh.config.SharedRsaKeyPairManager;
import com.zeromesh.kafka.SettlementEventProducer;
import com.zeromesh.model.MeshPacket;
import com.zeromesh.model.TransactionLedger;
import com.zeromesh.model.TransactionLedger.TransactionStatus;
import com.zeromesh.repository.TransactionLedgerRepository;
import com.zeromesh.service.idempotency.IdempotencyService;
import com.zeromesh.service.validation.PacketValidationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.PublicKey;

@Service
public class BridgeIngestionService {

    private final PacketValidationService     validationService;
    private final IdempotencyService          idempotencyService;
    private final SettlementEventProducer     settlementProducer;
    private final TransactionLedgerRepository ledgerRepository;
    private final SharedRsaKeyPairManager     keyManager;

    public BridgeIngestionService(
            PacketValidationService validationService,
            IdempotencyService idempotencyService,
            SettlementEventProducer settlementProducer,
            TransactionLedgerRepository ledgerRepository,
            SharedRsaKeyPairManager keyManager) {
        this.validationService  = validationService;
        this.idempotencyService = idempotencyService;
        this.settlementProducer = settlementProducer;
        this.ledgerRepository   = ledgerRepository;
        this.keyManager         = keyManager;
    }

    // RSA Public Key used to encrypt payment packets — Settlement Service holds matching private key
    public PublicKey getServerPublicKey() {
        return keyManager.getPublicKey();
    }

    /**
     * 4-Stage Ingestion Pipeline — total latency ~2-3ms.
     *
     * Stage 1: Expiry check    — reject packets older than 24h
     * Stage 2: Integrity check — reject if SHA-256(ciphertext) != packetId
     * Stage 3: Redis SETNX     — reject duplicate packet submissions
     * Stage 4: Kafka publish   — hand off to SettlementEventConsumer for ACID processing
     */
    public TransactionLedger ingest(MeshPacket packet) {
        String packetId = packet.getPacketId();

        // Stage 1: Expiry Check
        if (!validationService.isNotExpired(packet)) {
            System.out.println("[Ingestion] EXPIRED: " + packetId);
            return saveLedgerEntry(packetId, TransactionStatus.EXPIRED);
        }

        // Stage 2: Integrity Check
        if (!validationService.isIntegrityValid(packet)) {
            System.out.println("[Ingestion] TAMPERED: " + packetId);
            return saveLedgerEntry(packetId, TransactionStatus.TAMPERED);
        }

        // Stage 3: Redis Idempotency (SETNX)
        if (!idempotencyService.claim(packetId)) {
            System.out.println("[Ingestion] DUPLICATE: " + packetId);
            return saveLedgerEntry(packetId, TransactionStatus.DUPLICATE);
        }

        // Stage 4: Async hand-off to Kafka — settlement happens in SettlementEventConsumer
        settlementProducer.publishSettlementEvent(packet);
        System.out.println("[Ingestion] Queued for settlement: " + packetId);

        // Return a placeholder — actual ledger entry is written by SettlementEventConsumer
        return TransactionLedger.builder()
                .packetId(packetId)
                .senderId("QUEUED")
                .receiverId("QUEUED")
                .amount(BigDecimal.ZERO)
                .status(TransactionStatus.SETTLED)
                .build();
    }

    private TransactionLedger saveLedgerEntry(String packetId, TransactionStatus status) {
        String senderId   = status == TransactionStatus.DUPLICATE ? "REPLAY_ATTACK" : "REJECTED";
        String receiverId = status == TransactionStatus.DUPLICATE ? "BLOCKED"       : "REJECTED";

        TransactionLedger entry = TransactionLedger.builder()
                .packetId(packetId)
                .senderId(senderId)
                .receiverId(receiverId)
                .amount(BigDecimal.ZERO)
                .status(status)
                .build();
        try {
            return ledgerRepository.save(entry);
        } catch (Exception e) {
            // packetId already in DB (replay on an already-settled packet) — save audit row with timestamp suffix
            String auditId = packetId + "-dup-" + System.currentTimeMillis();
            TransactionLedger auditEntry = TransactionLedger.builder()
                    .packetId(auditId)
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .amount(BigDecimal.ZERO)
                    .status(status)
                    .build();
            try {
                return ledgerRepository.save(auditEntry);
            } catch (Exception ex) {
                return auditEntry;
            }
        }
    }
}
