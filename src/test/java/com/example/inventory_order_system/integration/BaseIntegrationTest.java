package com.example.inventory_order_system.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.redis.testcontainers.RedisContainer;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.flyway.enabled=false"
})
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("inventory_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
