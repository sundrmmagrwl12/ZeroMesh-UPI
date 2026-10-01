package com.zeromesh.dto;

import java.math.BigDecimal;

public class PaymentInstruction {

    private String senderId;
    private String receiverId;
    private BigDecimal amount;
    private String pinHash;
    private String nonce;
    private long timestamp;

    public PaymentInstruction() {}

    public PaymentInstruction(String senderId, String receiverId, BigDecimal amount,
                              String pinHash, String nonce, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.pinHash = pinHash;
        this.nonce = nonce;
        this.timestamp = timestamp;
    }

    public static PaymentInstructionBuilder builder() { return new PaymentInstructionBuilder(); }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public static class PaymentInstructionBuilder {
        private String senderId; private String receiverId; private BigDecimal amount;
        private String pinHash; private String nonce; private long timestamp;
        public PaymentInstructionBuilder senderId(String v) { this.senderId = v; return this; }
        public PaymentInstructionBuilder receiverId(String v) { this.receiverId = v; return this; }
        public PaymentInstructionBuilder amount(BigDecimal v) { this.amount = v; return this; }
        public PaymentInstructionBuilder pinHash(String v) { this.pinHash = v; return this; }
        public PaymentInstructionBuilder nonce(String v) { this.nonce = v; return this; }
        public PaymentInstructionBuilder timestamp(long v) { this.timestamp = v; return this; }
        public PaymentInstruction build() {
            return new PaymentInstruction(senderId, receiverId, amount, pinHash, nonce, timestamp);
        }
    }
}
