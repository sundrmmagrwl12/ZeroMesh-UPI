package com.zeromesh.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String SETTLEMENT_TOPIC = "mesh-payment-settlements";
    public static final String SETTLEMENT_DLT   = "mesh-payment-settlements-dlt";

    @Bean
    public NewTopic settlementTopic() {
        return TopicBuilder.name(SETTLEMENT_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic settlementDeadLetterTopic() {
        return TopicBuilder.name(SETTLEMENT_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
