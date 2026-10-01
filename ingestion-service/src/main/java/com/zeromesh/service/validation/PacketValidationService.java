package com.zeromesh.service.validation;

import com.zeromesh.model.MeshPacket;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Service
public class PacketValidationService {

    // Reject packets older than 24 hours
    private static final long MAX_PACKET_AGE_MS = 24 * 60 * 60 * 1000L;

    // Returns true if packet is within the 24-hour validity window
    public boolean isNotExpired(MeshPacket packet) {
        return (Instant.now().toEpochMilli() - packet.getCreatedAt()) <= MAX_PACKET_AGE_MS;
    }

    /**
     * Validates packet integrity by recomputing SHA-256(ciphertext) and comparing to packetId.
     * If they match, the ciphertext was not altered in transit.
     */
    public boolean isIntegrityValid(MeshPacket packet) {
        try {
            String ciphertext = packet.getPayload().getCiphertext();
            byte[] hashBytes = MessageDigest.getInstance("SHA-256")
                    .digest(ciphertext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes).equals(packet.getPacketId());
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }
}
