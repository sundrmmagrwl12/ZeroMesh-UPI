package com.zeromesh.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeromesh.config.KafkaTopicConfig;
import com.zeromesh.model.MeshPacket;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class SettlementEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SettlementEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                                   ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper  = objectMapper;
    }

    /**
     * Publishes a validated MeshPacket to the Kafka settlement topic.
     * packetId is used as the message key — guarantees same-packet ordering within a partition.
     */
    public void publishSettlementEvent(MeshPacket packet) {
        try {
            String payload = objectMapper.writeValueAsString(packet);
            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(KafkaTopicConfig.SETTLEMENT_TOPIC, packet.getPacketId(), payload);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    System.err.println("[Producer] Failed to publish: " + packet.getPacketId()
                            + " | " + ex.getMessage());
                } else {
                    System.out.println("[Producer] Published: " + packet.getPacketId()
                            + " → Partition: " + result.getRecordMetadata().partition()
                            + " | Offset: " + result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Kafka serialization failed: " + e.getMessage(), e);
        }
    }
}
