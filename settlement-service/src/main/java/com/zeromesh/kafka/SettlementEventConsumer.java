package com.zeromesh.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeromesh.config.KafkaTopicConfig;
import com.zeromesh.config.SharedRsaKeyPairManager;
import com.zeromesh.dto.PaymentInstruction;
import com.zeromesh.model.Account;
import com.zeromesh.model.MeshPacket;
import com.zeromesh.model.TransactionLedger;
import com.zeromesh.model.TransactionLedger.TransactionStatus;
import com.zeromesh.repository.AccountRepository;
import com.zeromesh.repository.TransactionLedgerRepository;
import com.zeromesh.service.crypto.CryptoService;
import com.zeromesh.service.idempotency.IdempotencyService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class SettlementEventConsumer {

    private final CryptoService               cryptoService;
    private final AccountRepository           accountRepository;
    private final TransactionLedgerRepository ledgerRepository;
    private final IdempotencyService          idempotencyService;
    private final ObjectMapper                objectMapper;
    private final SharedRsaKeyPairManager     keyManager;

    public SettlementEventConsumer(
            CryptoService cryptoService,
            AccountRepository accountRepository,
            TransactionLedgerRepository ledgerRepository,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper,
            SharedRsaKeyPairManager keyManager) {
        this.cryptoService      = cryptoService;
        this.accountRepository  = accountRepository;
        this.ledgerRepository   = ledgerRepository;
        this.idempotencyService = idempotencyService;
        this.objectMapper       = objectMapper;
        this.keyManager         = keyManager;
    }

    /**
     * Settles each payment packet from Kafka.
     * Uses the RSA private key from shared Docker volume to decrypt AES-GCM payload.
     * 3 retry attempts with exponential backoff (1s → 2s → 4s) before routing to DLT.
     */
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            autoCreateTopics = "true",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(
            topics = KafkaTopicConfig.SETTLEMENT_TOPIC,
            groupId = "zeromesh-settlement-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(ConsumerRecord<String, String> record) {
        String packetId = record.key();
        System.out.println("[Consumer] Received: " + packetId
                + " | Partition: " + record.partition()
                + " | Offset: " + record.offset());

        MeshPacket packet;
        try {
            packet = objectMapper.readValue(record.value(), MeshPacket.class);
        } catch (Exception e) {
            System.err.println("[Consumer] Deserialization failed: " + packetId + " | " + e.getMessage());
            saveLedger(packetId, "UNKNOWN", "UNKNOWN", BigDecimal.ZERO, TransactionStatus.TAMPERED);
            return;
        }

        try {
            PaymentInstruction instruction = cryptoService.decrypt(
                    packet.getPayload(), keyManager.getPrivateKey());

            String senderId   = instruction.getSenderId();
            String receiverId = instruction.getReceiverId();

            if (instruction.getAmount() == null || instruction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                idempotencyService.release(packetId);
                saveLedger(packetId, senderId, receiverId, BigDecimal.ZERO, TransactionStatus.TAMPERED);
                System.err.println("[Consumer] Invalid amount: " + instruction.getAmount());
                return;
            }

            Optional<Account> senderOpt   = accountRepository.findByUpiId(senderId);
            Optional<Account> receiverOpt = accountRepository.findByUpiId(receiverId);

            if (senderOpt.isEmpty() || receiverOpt.isEmpty()) {
                idempotencyService.release(packetId);
                saveLedger(packetId, senderId, receiverId, instruction.getAmount(), TransactionStatus.INVALID_ACCOUNT);
                System.err.println("[Consumer] Unknown account: " + senderId + " -> " + receiverId);
                return;
            }

            Account sender   = senderOpt.get();
            Account receiver = receiverOpt.get();

            if (sender.getBalance().compareTo(instruction.getAmount()) < 0) {
                idempotencyService.release(packetId);
                saveLedger(packetId, senderId, receiverId, instruction.getAmount(), TransactionStatus.INSUFFICIENT_FUNDS);
                System.err.println("[Consumer] Insufficient funds: " + senderId
                        + " has ₹" + sender.getBalance() + ", needs ₹" + instruction.getAmount());
                return;
            }

            // Atomic debit + credit — @Transactional ensures both succeed or both roll back
            sender.setBalance(sender.getBalance().subtract(instruction.getAmount()));
            receiver.setBalance(receiver.getBalance().add(instruction.getAmount()));
            accountRepository.save(sender);
            accountRepository.save(receiver);

            saveLedger(packetId, senderId, receiverId, instruction.getAmount(), TransactionStatus.SETTLED);
            idempotencyService.markSettled(packetId);

            System.out.println("[Consumer] SETTLED: " + senderId + " → " + receiverId
                    + " ₹" + instruction.getAmount());

        } catch (java.security.GeneralSecurityException e) {
            idempotencyService.release(packetId);
            System.err.println("[Consumer] Decryption failed (tampered payload): " + e.getMessage());
            saveLedger(packetId, "UNKNOWN", "UNKNOWN", BigDecimal.ZERO, TransactionStatus.TAMPERED);
        } catch (Exception e) {
            idempotencyService.release(packetId);
            System.err.println("[Consumer] Settlement error: " + e.getMessage());
            throw new RuntimeException("Settlement failed: " + e.getMessage(), e);
        }
    }

    // Dead Letter Topic handler — fires after all 3 retry attempts are exhausted
    @KafkaListener(
            topics = KafkaTopicConfig.SETTLEMENT_DLT,
            groupId = "zeromesh-dlt-group"
    )
    public void consumeDeadLetter(ConsumerRecord<String, String> record) {
        System.err.println("[DLT] Permanently failed: " + record.key() + " — requires manual investigation.");
    }

    // Upserts a ledger entry: updates existing row if found, otherwise inserts new
    private TransactionLedger saveLedger(String packetId, String senderId,
                                          String receiverId, BigDecimal amount,
                                          TransactionStatus status) {
        return ledgerRepository.findByPacketId(packetId).map(existing -> {
            existing.setSenderId(senderId);
            existing.setReceiverId(receiverId);
            existing.setAmount(amount);
            existing.setStatus(status);
            return ledgerRepository.save(existing);
        }).orElseGet(() -> ledgerRepository.save(
                TransactionLedger.builder()
                        .packetId(packetId)
                        .senderId(senderId)
                        .receiverId(receiverId)
                        .amount(amount)
                        .status(status)
                        .build()));
    }
}
