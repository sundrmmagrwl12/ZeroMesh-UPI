package com.zeromesh.controller;

import com.zeromesh.model.MeshPacket;
import com.zeromesh.model.TransactionLedger;
import com.zeromesh.model.VirtualDevice;
import com.zeromesh.service.crypto.CryptoService;
import com.zeromesh.service.ingestion.BridgeIngestionService;
import com.zeromesh.service.mesh.MeshSimulatorService;
import com.zeromesh.dto.PaymentInstruction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mesh")
public class MeshController {

    private final MeshSimulatorService simulatorService;
    private final CryptoService cryptoService;
    private final BridgeIngestionService ingestionService;
    private final com.zeromesh.service.idempotency.IdempotencyService idempotencyService;

    public MeshController(MeshSimulatorService simulatorService,
                          CryptoService cryptoService,
                          BridgeIngestionService ingestionService,
                          com.zeromesh.service.idempotency.IdempotencyService idempotencyService) {
        this.simulatorService   = simulatorService;
        this.cryptoService      = cryptoService;
        this.ingestionService   = ingestionService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Simulates a user creating an offline payment.
     * Encrypts the PaymentInstruction, wraps it in a MeshPacket,
     * computes packetId as SHA-256 of ciphertext, then injects into phone-alice.
     */
    @PostMapping("/inject")
    public ResponseEntity<Map<String, Object>> inject(@RequestBody Map<String, String> request) throws Exception {
        String senderId   = request.getOrDefault("senderId", "sundram@upi").trim().toLowerCase();
        String receiverId = request.getOrDefault("receiverId", "rahul@upi").trim().toLowerCase();
        if (!senderId.contains("@")) senderId += "@upi";
        if (!receiverId.contains("@")) receiverId += "@upi";
        String amount     = request.getOrDefault("amount", "500.00").trim();

        BigDecimal parsedAmount;
        try {
            parsedAmount = new BigDecimal(amount);
            if (parsedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Payment amount must be greater than zero"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid numeric amount: " + amount));
        }

        if (senderId.equalsIgnoreCase(receiverId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sender and receiver cannot be the same account"));
        }

        PaymentInstruction instruction = PaymentInstruction.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .amount(parsedAmount)
                .pinHash("hashed-pin-demo")
                .nonce("nonce-" + System.currentTimeMillis())
                .timestamp(Instant.now().toEpochMilli())
                .build();

        var encryptedPayload = cryptoService.encrypt(instruction, ingestionService.getServerPublicKey());

        // packetId = SHA-256 of ciphertext — acts as tamper-proof fingerprint
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(
                encryptedPayload.getCiphertext().getBytes(StandardCharsets.UTF_8));
        String packetId = Base64.getEncoder().encodeToString(hashBytes);

        MeshPacket packet = MeshPacket.builder()
                .packetId(packetId)
                .ttl(5)
                .createdAt(Instant.now().toEpochMilli())
                .payload(encryptedPayload)
                .build();

        simulatorService.injectPacket(packet);

        return ResponseEntity.ok(Map.of(
                "message",  "Packet injected into phone-alice",
                "packetId", packetId,
                "ttl",      5
        ));
    }

    // Runs one round of Bluetooth gossip across all virtual devices
    @PostMapping("/gossip")
    public ResponseEntity<Map<String, Object>> gossip() {
        simulatorService.runGossipRound();
        List<VirtualDevice> status = simulatorService.getNetworkStatus();
        return ResponseEntity.ok(Map.of(
                "message", "Gossip round complete",
                "devicePacketCounts", status.stream()
                        .map(d -> Map.of("device", d.getName(), "packets", d.getPackets().size()))
                        .toList()
        ));
    }

    // Bridge device got internet — upload all held packets through the ingestion pipeline
    @PostMapping("/flush")
    public ResponseEntity<Map<String, Object>> flush() {
        List<MeshPacket> packets = simulatorService.flushBridges();
        List<TransactionLedger> results = packets.stream()
                .map(ingestionService::ingest)
                .filter(java.util.Objects::nonNull)
                .toList();

        return ResponseEntity.ok(Map.of(
                "message",          "Bridge flush complete",
                "packetsProcessed", packets.size(),
                "results", results.stream()
                        .map(r -> Map.of(
                                "packetId", r.getPacketId() != null ? r.getPacketId() : "",
                                "status",   r.getStatus()   != null ? r.getStatus().name() : "SETTLED"
                        ))
                        .toList()
        ));
    }

    // Resets the mesh network — clears all devices for a fresh demo run
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        simulatorService.resetNetwork();
        idempotencyService.clearMemory();
        return ResponseEntity.ok(Map.of("message", "Mesh network reset successfully"));
    }

    // Returns live packet count on each virtual device
    @GetMapping("/status")
    public ResponseEntity<List<Map<String, Object>>> status() {
        return ResponseEntity.ok(simulatorService.getNetworkStatus().stream()
                .map(d -> Map.<String, Object>of(
                        "device",      d.getName(),
                        "hasInternet", d.hasInternet(),
                        "packetCount", d.getPackets().size()
                ))
                .toList());
    }
}
