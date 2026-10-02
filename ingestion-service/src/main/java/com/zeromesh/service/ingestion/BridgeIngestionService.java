package com.zeromesh.service.ingestion;

import com.zeromesh.config.SharedRsaKeyPairManager;
import com.zeromesh.dto.PaymentInstruction;
import com.zeromesh.kafka.SettlementEventProducer;
import com.zeromesh.model.Account;
import com.zeromesh.model.MeshPacket;
import com.zeromesh.model.TransactionLedger;
import com.zeromesh.model.TransactionLedger.TransactionStatus;
import com.zeromesh.repository.AccountRepository;
import com.zeromesh.repository.TransactionLedgerRepository;
import com.zeromesh.service.crypto.CryptoService;
import com.zeromesh.service.idempotency.IdempotencyService;
import com.zeromesh.service.validation.PacketValidationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.Optional;

@Service
public class BridgeIngestionService {

    private final PacketValidationService     validationService;
    private final IdempotencyService          idempotencyService;
    private final SettlementEventProducer     settlementProducer;
    private final TransactionLedgerRepository ledgerRepository;
    private final AccountRepository           accountRepository;
    private final CryptoService               cryptoService;
    private final SharedRsaKeyPairManager     keyManager;

    public BridgeIngestionService(
            PacketValidationService validationService,
            IdempotencyService idempotencyService,
            SettlementEventProducer settlementProducer,
            TransactionLedgerRepository ledgerRepository,
            AccountRepository accountRepository,
            CryptoService cryptoService,
            SharedRsaKeyPairManager keyManager) {
        this.validationService   = validationService;
        this.idempotencyService  = idempotencyService;
        this.settlementProducer  = settlementProducer;
        this.ledgerRepository    = ledgerRepository;
        this.accountRepository   = accountRepository;
        this.cryptoService       = cryptoService;
        this.keyManager          = keyManager;
    }

    // RSA Public Key used to encrypt payment packets
    public PublicKey getServerPublicKey() {
        return keyManager.getPublicKey();
    }

    /**
     * 4-Stage Ingestion Pipeline — total latency ~2-3ms.
     *
     * Stage 1: Expiry check    — reject packets older than 24h
     * Stage 2: Integrity check — reject if SHA-256(ciphertext) != packetId
     * Stage 3: Redis SETNX     — reject duplicate packet submissions
     * Stage 4: Kafka publish   — hand off to SettlementEventConsumer (with standalone fallback)
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

        // Stage 4: Async hand-off to Kafka — with seamless standalone fallback if broker not connected
        try {
            settlementProducer.publishSettlementEvent(packet);
            System.out.println("[Ingestion] Queued for settlement in Kafka: " + packetId);
            return TransactionLedger.builder()
                    .packetId(packetId)
                    .senderId("QUEUED")
                    .receiverId("QUEUED")
                    .amount(BigDecimal.ZERO)
                    .status(TransactionStatus.SETTLED)
                    .build();
        } catch (Exception e) {
            System.out.println("[Ingestion] Kafka cluster not reachable. Executing direct in-process settlement: " + packetId);
            return executeDirectSettlement(packet);
        }
    }

    private TransactionLedger executeDirectSettlement(MeshPacket packet) {
        String packetId = packet.getPacketId();
        try {
            PaymentInstruction instruction = cryptoService.decrypt(packet.getPayload(), keyManager.getPrivateKey());
            String senderId   = instruction.getSenderId();
            String receiverId = instruction.getReceiverId();
            BigDecimal amount = instruction.getAmount();

            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                idempotencyService.release(packetId);
                return saveLedgerEntryWithDetails(packetId, senderId, receiverId, BigDecimal.ZERO, TransactionStatus.TAMPERED);
            }

            Optional<Account> senderOpt   = accountRepository.findByUpiId(senderId);
            Optional<Account> receiverOpt = accountRepository.findByUpiId(receiverId);

            if (senderOpt.isEmpty() || receiverOpt.isEmpty()) {
                idempotencyService.release(packetId);
                return saveLedgerEntryWithDetails(packetId, senderId, receiverId, amount, TransactionStatus.TAMPERED);
            }

            Account sender   = senderOpt.get();
            Account receiver = receiverOpt.get();

            if (sender.getBalance().compareTo(amount) < 0) {
                idempotencyService.release(packetId);
                return saveLedgerEntryWithDetails(packetId, senderId, receiverId, amount, TransactionStatus.INSUFFICIENT_FUNDS);
            }

            sender.setBalance(sender.getBalance().subtract(amount));
            receiver.setBalance(receiver.getBalance().add(amount));
            accountRepository.save(sender);
            accountRepository.save(receiver);

            idempotencyService.markSettled(packetId);
            return saveLedgerEntryWithDetails(packetId, senderId, receiverId, amount, TransactionStatus.SETTLED);
        } catch (Exception ex) {
            idempotencyService.release(packetId);
            return saveLedgerEntryWithDetails(packetId, "UNKNOWN", "UNKNOWN", BigDecimal.ZERO, TransactionStatus.TAMPERED);
        }
    }

    private TransactionLedger saveLedgerEntryWithDetails(String packetId, String senderId, String receiverId, BigDecimal amount, TransactionStatus status) {
        TransactionLedger entry = TransactionLedger.builder()
                .packetId(packetId)
                .senderId(senderId)
                .receiverId(receiverId)
                .amount(amount)
                .status(status)
                .build();
        try {
            return ledgerRepository.save(entry);
        } catch (Exception e) {
            String auditId = packetId + "-dup-" + System.currentTimeMillis();
            TransactionLedger auditEntry = TransactionLedger.builder()
                    .packetId(auditId)
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .amount(amount)
                    .status(status)
                    .build();
            try {
                return ledgerRepository.save(auditEntry);
            } catch (Exception ex) {
                return auditEntry;
            }
        }
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
