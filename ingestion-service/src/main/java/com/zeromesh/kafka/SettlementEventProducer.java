package com.zeromesh.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeromesh.config.KafkaTopicConfig;
import com.zeromesh.model.MeshPacket;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
     * Waits up to 600ms for broker ack — if Kafka is unavailable, throws exception to trigger fallback.
     */
    public void publishSettlementEvent(MeshPacket packet) throws Exception {
        String payload = objectMapper.writeValueAsString(packet);
        CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(KafkaTopicConfig.SETTLEMENT_TOPIC, packet.getPacketId(), payload);

        future.get(600, TimeUnit.MILLISECONDS);
    }
}
