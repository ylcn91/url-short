package com.urlshort.event;

import com.urlshort.dto.ClickEventDto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service to publish click events to Kafka asynchronously.
 *
 * Key features:
 * - Async fire-and-forget publishing (< 5ms overhead)
 * - Error handling with logging (doesn't fail redirect on Kafka failure)
 * - Metrics for published events and errors
 * - Dead-letter queue for failed events
 *
 * Usage:
 * This service is called from the RedirectController after a successful redirect.
 * Publishing is asynchronous and does not block the HTTP response.
 */
@Service
public class ClickEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ClickEventProducer.class);

    private final KafkaTemplate<String, ClickEventDto> kafkaTemplate;
    private final String topicName;
    private final String dlqTopicName;
    private final Counter publishedCounter;
    private final Counter failedCounter;

    public ClickEventProducer(
            KafkaTemplate<String, ClickEventDto> kafkaTemplate,
            @Value("${spring.kafka.topic.click-events.name}") String topicName,
            MeterRegistry meterRegistry
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
        this.dlqTopicName = topicName + "-dlq";

        // Initialize metrics
        this.publishedCounter = Counter.builder("kafka.click.events.published")
                .description("Total number of click events published to Kafka")
                .register(meterRegistry);

        this.failedCounter = Counter.builder("kafka.click.events.failed")
                .description("Total number of failed click event publications")
                .register(meterRegistry);
    }

    /**
     * Publish a click event to Kafka asynchronously.
     *
     * This method is fire-and-forget with error handling:
     * - Events are published to the click-events topic
     * - Partitioned by short_link_id for ordered processing
     * - Failures are logged and sent to dead-letter queue
     * - Does NOT block the calling thread
     *
     * @param event the click event to publish
     */
    public void publishClickEvent(ClickEventDto event) {
        if (event == null) {
            log.warn("Attempted to publish null click event");
            return;
        }

        try {
            // Use short_link_id as partition key to ensure all clicks for the same link
            // go to the same partition, enabling ordered processing
            String partitionKey = String.valueOf(event.getShortLinkId());

            // Async send - returns immediately
            CompletableFuture<SendResult<String, ClickEventDto>> future =
                    kafkaTemplate.send(topicName, partitionKey, event);

            // Handle success/failure asynchronously
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    handleSuccess(event, result);
                } else {
                    handleFailure(event, ex);
                }
            });

        } catch (Exception ex) {
            log.error("Unexpected error publishing click event: clickId={}, shortLinkId={}",
                    event.getClickId(), event.getShortLinkId(), ex);
            failedCounter.increment();
        }
    }

    /**
     * Handle successful event publication.
     */
    private void handleSuccess(ClickEventDto event, SendResult<String, ClickEventDto> result) {
        publishedCounter.increment();

        if (log.isDebugEnabled()) {
            var metadata = result.getRecordMetadata();
            log.debug("Click event published successfully: clickId={}, shortLinkId={}, " +
                            "topic={}, partition={}, offset={}",
                    event.getClickId(),
                    event.getShortLinkId(),
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset());
        }

        // Log at INFO level for monitoring (structured logging)
        log.info("Click event published: clickId={}, shortLinkId={}, workspaceId={}, country={}, deviceType={}",
                event.getClickId(),
                event.getShortLinkId(),
                event.getWorkspaceId(),
                event.getCountry(),
                event.getDeviceType());
    }

    /**
     * Handle failed event publication.
     *
     * Failures are logged and the event is sent to a dead-letter queue for manual review.
     * The redirect is NOT affected - this is a best-effort analytics mechanism.
     */
    private void handleFailure(ClickEventDto event, Throwable ex) {
        failedCounter.increment();

        log.error("Failed to publish click event: clickId={}, shortLinkId={}, workspaceId={}, error={}",
                event.getClickId(),
                event.getShortLinkId(),
                event.getWorkspaceId(),
                ex.getMessage(),
                ex);

        // Send to dead-letter queue for manual intervention
        sendToDeadLetterQueue(event);
    }

    /**
     * Send failed event to dead-letter queue.
     *
     * Best-effort attempt to preserve the event for later analysis.
     * If DLQ send also fails, the event is lost (acceptable for analytics use case).
     */
    private void sendToDeadLetterQueue(ClickEventDto event) {
        try {
            kafkaTemplate.send(dlqTopicName, event.getClickId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Click event sent to DLQ: clickId={}, shortLinkId={}",
                                    event.getClickId(), event.getShortLinkId());
                        } else {
                            log.error("Failed to send click event to DLQ: clickId={}, shortLinkId={}",
                                    event.getClickId(), event.getShortLinkId(), ex);
                        }
                    });
        } catch (Exception ex) {
            log.error("Unexpected error sending to DLQ: clickId={}", event.getClickId(), ex);
        }
    }

    /**
     * Publish a click event synchronously (blocking).
     *
     * Use this only in test scenarios or when you need guaranteed delivery
     * before proceeding. NOT recommended for production redirect flow.
     *
     * @param event the click event to publish
     * @throws Exception if publication fails
     */
    public void publishClickEventSync(ClickEventDto event) throws Exception {
        String partitionKey = String.valueOf(event.getShortLinkId());
        SendResult<String, ClickEventDto> result = kafkaTemplate.send(topicName, partitionKey, event).get();
        handleSuccess(event, result);
    }
}
