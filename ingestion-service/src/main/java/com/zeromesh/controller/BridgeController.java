package com.zeromesh.controller;

import com.zeromesh.dto.EncryptedPayloadDto;
import com.zeromesh.model.MeshPacket;
import com.zeromesh.model.TransactionLedger;
import com.zeromesh.service.idempotency.IdempotencyService;
import com.zeromesh.service.ingestion.BridgeIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bridge")
public class BridgeController {

    private final BridgeIngestionService ingestionService;
    private final IdempotencyService idempotencyService;

    public BridgeController(BridgeIngestionService ingestionService,
                            IdempotencyService idempotencyService) {
        this.ingestionService   = ingestionService;
        this.idempotencyService = idempotencyService;
    }

    // Single packet upload — runs full 4-stage security pipeline
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingest(@RequestBody MeshPacket packet) {
        TransactionLedger result = ingestionService.ingest(packet);
        return ResponseEntity.ok(Map.of(
                "packetId",   result.getPacketId() != null ? result.getPacketId() : "",
                "status",     result.getStatus().name(),
                "settledAt",  result.getSettledAt() != null ? result.getSettledAt().toString() : ""
        ));
    }

    // Batch upload — multiple packets flushed at once from bridge device
    @PostMapping("/ingest/batch")
    public ResponseEntity<List<Map<String, Object>>> ingestBatch(@RequestBody List<MeshPacket> packets) {
        List<Map<String, Object>> results = packets.stream()
                .map(packet -> {
                    TransactionLedger result = ingestionService.ingest(packet);
                    return Map.<String, Object>of(
                            "packetId", result.getPacketId(),
                            "status",   result.getStatus().name()
                    );
                })
                .toList();
        return ResponseEntity.ok(results);
    }

    // --- Chaos Engine: Security Attack Simulations ---

    // Tamper Attack — ciphertext altered in transit, Stage-2 SHA-256 check catches it
    @PostMapping("/attack/tamper")
    public ResponseEntity<Map<String, Object>> attackTamper() {
        MeshPacket packet = MeshPacket.builder()
                .packetId("TAMPER-ATTACK-" + System.currentTimeMillis())
                .ttl(5)
                .createdAt(System.currentTimeMillis())
                .payload(new EncryptedPayloadDto("attacker-key", "attacker-iv", "altered-tampered-content"))
                .build();
        TransactionLedger result = ingestionService.ingest(packet);
        return ResponseEntity.ok(Map.of(
                "packetId", result.getPacketId() != null ? result.getPacketId() : "",
                "status",   result.getStatus().name(),
                "message",  "Stage-2 Checksum Mismatch: Altered ciphertext detected and rejected as TAMPERED"
        ));
    }

    // Replay Attack — same packetId submitted twice, Stage-3 Redis SETNX blocks it
    @PostMapping("/attack/replay")
    public ResponseEntity<Map<String, Object>> attackReplay() {
        String sampleCiphertext = "replay-attack-sample-data";
        String packetId = computeSha256(sampleCiphertext);

        // Pre-claim in Redis so the ingestion pipeline always hits Stage-3 duplicate check
        idempotencyService.claim(packetId);

        MeshPacket packet = MeshPacket.builder()
                .packetId(packetId)
                .ttl(5)
                .createdAt(System.currentTimeMillis())
                .payload(new EncryptedPayloadDto("key", "iv", sampleCiphertext))
                .build();

        TransactionLedger result = ingestionService.ingest(packet);
        return ResponseEntity.ok(Map.of(
                "packetId", result.getPacketId() != null ? result.getPacketId() : "",
                "status",   result.getStatus().name(),
                "message",  "Stage-3 Redis Lock: Duplicate packet blocked by Redis SETNX"
        ));
    }

    // Expired Packet — createdAt set to Year 2001, Stage-1 expiry check rejects it
    @PostMapping("/attack/expired")
    public ResponseEntity<Map<String, Object>> attackExpired() {
        MeshPacket packet = MeshPacket.builder()
                .packetId("EXPIRED-PACKET-" + System.currentTimeMillis())
                .ttl(5)
                .createdAt(1000000000000L) // ~Sep 2001 — way beyond 24h window
                .payload(new EncryptedPayloadDto("key", "iv", "expired-payload-data"))
                .build();
        TransactionLedger result = ingestionService.ingest(packet);
        return ResponseEntity.ok(Map.of(
                "packetId", result.getPacketId() != null ? result.getPacketId() : "",
                "status",   result.getStatus().name(),
                "message",  "Stage-1 Expiry Check: Packet age exceeded 24-hour validity window"
        ));
    }

    private String computeSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
