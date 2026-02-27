package com.urlshort.config;

import com.urlshort.dto.event.ClickEventDto;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for click event tracking.
 * Configures:
 * - Producer for publishing click events asynchronously
 * - Consumer for processing click events from Kafka
 * - Topic creation with appropriate partitions and replication
 * - Serialization/deserialization (JSON)
 * - Error handling and retry strategies
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.topic.click-events.name}")
    private String clickEventsTopic;

    @Value("${spring.kafka.topic.click-events.partitions:12}")
    private int clickEventsPartitions;

    @Value("${spring.kafka.topic.click-events.replication-factor:2}")
    private short clickEventsReplicationFactor;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    // ============================================================================
    // PRODUCER CONFIGURATION
    // ============================================================================

    /**
     * Producer configuration for click events.
     * - acks=all: Wait for all replicas to acknowledge (durability)
     * - compression=snappy: Compress messages for network efficiency
     * - linger.ms=10: Batch messages for 10ms before sending
     * - batch.size=32KB: Maximum batch size
     */
    @Bean
    public ProducerFactory<String, ClickEventDto> clickEventProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Durability settings
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Performance settings
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768); // 32KB
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864); // 64MB

        // Timeout settings
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, ClickEventDto> clickEventKafkaTemplate() {
        return new KafkaTemplate<>(clickEventProducerFactory());
    }

    // ============================================================================
    // CONSUMER CONFIGURATION
    // ============================================================================

    /**
     * Consumer configuration for click events.
     * - Manual offset commit: Commit after successful batch processing
     * - Error handling deserializer: Handle deserialization errors gracefully
     * - max.poll.records=500: Process up to 500 records per poll
     */
    @Bean
    public ConsumerFactory<String, ClickEventDto> clickEventConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // Consumer behavior
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutes

        // JSON deserialization settings
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.urlshort.dto");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ClickEventDto.class.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ClickEventDto> clickEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ClickEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(clickEventConsumerFactory());

        // Manual acknowledgment mode for batch processing
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // Concurrency: Number of consumer threads (should match partition count)
        factory.setConcurrency(6);

        // Batch processing
        factory.setBatchListener(false); // Process one record at a time for simplicity

        return factory;
    }

    // ============================================================================
    // TOPIC CONFIGURATION
    // ============================================================================

    /**
     * Topic: click-events
     * - Primary event stream for all click events
     * - Partitioned by short_link_id for ordered processing
     * - Retention: 30 days (configurable via broker settings)
     */
    @Bean
    public NewTopic clickEventsTopic() {
        return TopicBuilder.name(clickEventsTopic)
                .partitions(clickEventsPartitions)
                .replicas(clickEventsReplicationFactor)
                .compact() // Enable log compaction
                .config("retention.ms", "2592000000") // 30 days retention
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "1")
                .build();
    }

    /**
     * Topic: click-events-dlq
     * - Dead Letter Queue for failed click event processing
     * - Lower partition count as DLQ should have minimal traffic
     */
    @Bean
    public NewTopic clickEventsDlqTopic() {
        return TopicBuilder.name(clickEventsTopic + "-dlq")
                .partitions(3)
                .replicas(clickEventsReplicationFactor)
                .config("retention.ms", "7776000000") // 90 days retention
                .build();
    }
}
