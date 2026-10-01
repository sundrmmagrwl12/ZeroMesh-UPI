package com.zeromesh.model;

import com.zeromesh.dto.EncryptedPayloadDto;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Outer envelope that carries an encrypted payment across the Bluetooth mesh network.
 *
 * packetId  — SHA-256(ciphertext), unique fingerprint, used by Redis for dedup
 * ttl       — remaining hop count (starts at 5, packet stops forwarding at 0)
 * createdAt — epoch ms when payment was created; server rejects if older than 24h
 * payload   — RSA+AES-GCM encrypted PaymentInstruction
 */
public class MeshPacket {

    private String packetId;
    private int ttl;
    private long createdAt;
    private EncryptedPayloadDto payload;

    public MeshPacket() {}

    public MeshPacket(String packetId, int ttl, long createdAt, EncryptedPayloadDto payload) {
        this.packetId  = packetId;
        this.ttl       = ttl;
        this.createdAt = createdAt;
        this.payload   = payload;
    }

    public static MeshPacketBuilder builder() {
        return new MeshPacketBuilder();
    }

    public static class MeshPacketBuilder {
        private String packetId;
        private int ttl;
        private long createdAt;
        private EncryptedPayloadDto payload;

        public MeshPacketBuilder packetId(String v)              { this.packetId  = v; return this; }
        public MeshPacketBuilder ttl(int v)                      { this.ttl       = v; return this; }
        public MeshPacketBuilder createdAt(long v)               { this.createdAt = v; return this; }
        public MeshPacketBuilder payload(EncryptedPayloadDto v)  { this.payload   = v; return this; }
        public MeshPacket build() {
            return new MeshPacket(packetId, ttl, createdAt, payload);
        }
    }

    public String getPacketId()            { return packetId; }
    public void setPacketId(String v)      { this.packetId = v; }
    public int getTtl()                    { return ttl; }
    public void setTtl(int v)             { this.ttl = v; }
    public long getCreatedAt()             { return createdAt; }
    public void setCreatedAt(long v)       { this.createdAt = v; }
    public EncryptedPayloadDto getPayload(){ return payload; }
    public void setPayload(EncryptedPayloadDto v) { this.payload = v; }

    // True if packet still has remaining hops left to forward
    @JsonIgnore
    public boolean isForwardable() {
        return this.ttl > 0;
    }

    // Returns a new MeshPacket with ttl decremented by 1 (immutable — original unchanged)
    public MeshPacket withDecrementedTtl() {
        return MeshPacket.builder()
                .packetId(this.packetId)
                .ttl(this.ttl - 1)
                .createdAt(this.createdAt)
                .payload(this.payload)
                .build();
    }

    @Override
    public String toString() {
        return "MeshPacket{packetId='" + packetId + "', ttl=" + ttl + ", createdAt=" + createdAt + "}";
    }
}
