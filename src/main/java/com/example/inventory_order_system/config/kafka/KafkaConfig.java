package com.example.inventory_order_system.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    public static final String TOPIC_ORDERS_COMPLETED = "orders.completed";
    public static final String TOPIC_ORDERS_PLACED = "orders.placed";

    // 1. Define Producer Configuration
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    // 2. Define the KafkaTemplate Bean (This is what OrderService is looking for)
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public NewTopic ordersCompletedTopic() {
        return TopicBuilder.name(TOPIC_ORDERS_COMPLETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ordersPlacedTopic() {
        return TopicBuilder.name(TOPIC_ORDERS_PLACED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}