package com.zeromesh.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Permanent audit log of every payment packet processed by ZeroMesh-UPI.
 * Maps to the "transaction_ledger" table in PostgreSQL.
 */
@Entity
@Table(name = "transaction_ledger")
public class TransactionLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SHA-256(ciphertext) — unique fingerprint of the MeshPacket
    @Column(name = "packet_id", nullable = false, unique = true)
    private String packetId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "receiver_id", nullable = false)
    private String receiverId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    // Timestamp of when the server processed this packet
    @Column(name = "settled_at", nullable = false)
    private LocalDateTime settledAt;

    public enum TransactionStatus {
        SETTLED,            // Payment successfully processed
        DUPLICATE,          // Rejected by Redis idempotency check
        EXPIRED,            // Packet older than 24 hours
        TAMPERED,           // SHA-256 checksum mismatch or AES-GCM auth tag failure
        INSUFFICIENT_FUNDS, // Sender balance too low
        INVALID_ACCOUNT     // Account not found in core banking system
    }

    public TransactionLedger() {}

    public TransactionLedger(String packetId, String senderId, String receiverId,
                             BigDecimal amount, TransactionStatus status) {
        this.packetId   = packetId;
        this.senderId   = senderId;
        this.receiverId = receiverId;
        this.amount     = amount;
        this.status     = status;
        this.settledAt  = LocalDateTime.now();
    }

    public static TransactionLedgerBuilder builder() {
        return new TransactionLedgerBuilder();
    }

    public static class TransactionLedgerBuilder {
        private String packetId;
        private String senderId;
        private String receiverId;
        private BigDecimal amount;
        private TransactionStatus status;

        public TransactionLedgerBuilder packetId(String v)             { this.packetId   = v; return this; }
        public TransactionLedgerBuilder senderId(String v)             { this.senderId   = v; return this; }
        public TransactionLedgerBuilder receiverId(String v)           { this.receiverId = v; return this; }
        public TransactionLedgerBuilder amount(BigDecimal v)           { this.amount     = v; return this; }
        public TransactionLedgerBuilder status(TransactionStatus v)    { this.status     = v; return this; }
        public TransactionLedger build() {
            return new TransactionLedger(packetId, senderId, receiverId, amount, status);
        }
    }

    public Long getId()                   { return id; }
    public String getPacketId()           { return packetId; }
    public String getSenderId()           { return senderId; }
    public String getReceiverId()         { return receiverId; }
    public BigDecimal getAmount()         { return amount; }
    public TransactionStatus getStatus()  { return status; }
    public LocalDateTime getSettledAt()   { return settledAt; }

    public void setSenderId(String senderId)       { this.senderId   = senderId; }
    public void setReceiverId(String receiverId)   { this.receiverId = receiverId; }
    public void setAmount(BigDecimal amount)       { this.amount     = amount; }
    public void setStatus(TransactionStatus status){ this.status     = status; }
    public void setSettledAt(LocalDateTime t)      { this.settledAt  = t; }

    @Override
    public String toString() {
        return "TransactionLedger{packetId='" + packetId + "', sender='" + senderId
                + "', receiver='" + receiverId + "', amount=" + amount
                + ", status=" + status + ", settledAt=" + settledAt + "}";
    }
}
