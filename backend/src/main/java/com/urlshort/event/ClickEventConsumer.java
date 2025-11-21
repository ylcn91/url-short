package com.urlshort.event;

import com.urlshort.domain.ClickEvent;
import com.urlshort.domain.DeviceType;
import com.urlshort.domain.ShortLink;
import com.urlshort.dto.ClickEventDto;
import com.urlshort.repository.ClickEventRepository;
import com.urlshort.repository.ShortLinkRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Consumer to process click events from Kafka.
 *
 * Key features:
 * - Consumes events from "click-events" topic
 * - Batch processing for efficiency
 * - Saves to click_event table in PostgreSQL
 * - Manual commit after successful processing
 * - Error handling with dead-letter queue
 * - Metrics for consumed events and errors
 *
 * Consumer Group: click-analytics-processor
 */
@Service
public class ClickEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ClickEventConsumer.class);

    private final ClickEventRepository clickEventRepository;
    private final ShortLinkRepository shortLinkRepository;
    private final Counter consumedCounter;
    private final Counter errorCounter;
    private final Timer processingTimer;

    public ClickEventConsumer(
            ClickEventRepository clickEventRepository,
            ShortLinkRepository shortLinkRepository,
            MeterRegistry meterRegistry
    ) {
        this.clickEventRepository = clickEventRepository;
        this.shortLinkRepository = shortLinkRepository;

        // Initialize metrics
        this.consumedCounter = Counter.builder("kafka.click.events.consumed")
                .description("Total number of click events consumed from Kafka")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("kafka.click.events.consumer.errors")
                .description("Total number of click event processing errors")
                .register(meterRegistry);

        this.processingTimer = Timer.builder("kafka.click.events.processing.time")
                .description("Time taken to process click events")
                .register(meterRegistry);
    }

    /**
     * Consume click events from Kafka and save to PostgreSQL.
     *
     * This method:
     * - Listens to the "click-events" topic
     * - Processes events one at a time
     * - Converts DTO to entity and saves to database
     * - Manually acknowledges after successful save
     * - Logs errors and increments error metrics on failure
     *
     * @param eventDto the click event DTO from Kafka
     * @param acknowledgment manual acknowledgment handle
     * @param partition the Kafka partition
     * @param offset the message offset
     */
    @KafkaListener(
            topics = "${spring.kafka.topic.click-events.name}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "clickEventKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeClickEvent(
            @Payload ClickEventDto eventDto,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        Timer.Sample sample = Timer.start();

        try {
            log.debug("Consuming click event: clickId={}, shortLinkId={}, partition={}, offset={}",
                    eventDto.getClickId(), eventDto.getShortLinkId(), partition, offset);

            // Convert DTO to entity and save
            ClickEvent clickEvent = convertToEntity(eventDto);

            if (clickEvent != null) {
                clickEventRepository.save(clickEvent);
                consumedCounter.increment();

                log.info("Click event processed: clickId={}, shortLinkId={}, workspaceId={}, " +
                                "country={}, deviceType={}, partition={}, offset={}",
                        eventDto.getClickId(),
                        eventDto.getShortLinkId(),
                        eventDto.getWorkspaceId(),
                        eventDto.getCountry(),
                        eventDto.getDeviceType(),
                        partition,
                        offset);

                // Manual acknowledgment - commit offset after successful processing
                acknowledgment.acknowledge();
            } else {
                log.warn("Failed to convert click event to entity: clickId={}, shortLinkId={}",
                        eventDto.getClickId(), eventDto.getShortLinkId());
                errorCounter.increment();

                // Still acknowledge to avoid reprocessing invalid data
                acknowledgment.acknowledge();
            }

        } catch (Exception ex) {
            errorCounter.increment();

            log.error("Error processing click event: clickId={}, shortLinkId={}, partition={}, " +
                            "offset={}, error={}",
                    eventDto.getClickId(),
                    eventDto.getShortLinkId(),
                    partition,
                    offset,
                    ex.getMessage(),
                    ex);

            // Do NOT acknowledge - message will be retried or sent to DLQ
            // based on Kafka consumer configuration
            throw new RuntimeException("Failed to process click event", ex);

        } finally {
            sample.stop(processingTimer);
        }
    }

    /**
     * Batch consumption alternative (if enabled in configuration).
     *
     * This method processes multiple events in a single transaction for better performance.
     * Use this when event volume is high (>1000 events/second).
     *
     * @param events list of click event DTOs
     * @param acknowledgment manual acknowledgment handle
     */
    @Transactional
    public void consumeClickEventsBatch(
            List<ClickEventDto> events,
            Acknowledgment acknowledgment
    ) {
        Timer.Sample sample = Timer.start();
        List<ClickEvent> clickEvents = new ArrayList<>();

        try {
            log.debug("Consuming batch of {} click events", events.size());

            // Convert all DTOs to entities
            for (ClickEventDto eventDto : events) {
                ClickEvent clickEvent = convertToEntity(eventDto);
                if (clickEvent != null) {
                    clickEvents.add(clickEvent);
                }
            }

            // Batch save for better performance
            clickEventRepository.saveAll(clickEvents);
            consumedCounter.increment(clickEvents.size());

            log.info("Batch processed: {} click events saved", clickEvents.size());

            // Acknowledge the entire batch
            acknowledgment.acknowledge();

        } catch (Exception ex) {
            errorCounter.increment();
            log.error("Error processing click event batch: size={}, error={}",
                    events.size(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to process click event batch", ex);

        } finally {
            sample.stop(processingTimer);
        }
    }

    /**
     * Convert ClickEventDto to ClickEvent entity.
     *
     * This method:
     * - Looks up the ShortLink entity by ID
     * - Maps DTO fields to entity fields
     * - Converts device type string to enum
     * - Returns null if ShortLink not found (data inconsistency)
     *
     * @param dto the click event DTO from Kafka
     * @return the ClickEvent entity, or null if conversion fails
     */
    private ClickEvent convertToEntity(ClickEventDto dto) {
        // Look up the short link
        Optional<ShortLink> shortLinkOpt = shortLinkRepository.findById(dto.getShortLinkId());

        if (shortLinkOpt.isEmpty()) {
            log.warn("ShortLink not found for click event: clickId={}, shortLinkId={}",
                    dto.getClickId(), dto.getShortLinkId());
            return null;
        }

        ShortLink shortLink = shortLinkOpt.get();

        // Convert device type string to enum
        DeviceType deviceType = parseDeviceType(dto.getDeviceType());

        // Build the ClickEvent entity
        return ClickEvent.builder()
                .shortLink(shortLink)
                .clickedAt(dto.getTimestamp())
                .ipAddress(dto.getIp())
                .userAgent(dto.getUserAgent())
                .referer(dto.getReferrer())
                .country(dto.getCountry())
                .city(dto.getCity())
                .deviceType(deviceType)
                .browser(dto.getBrowser())
                .os(dto.getOs())
                .build();
    }

    /**
     * Parse device type string to DeviceType enum.
     *
     * @param deviceTypeStr the device type string from DTO
     * @return the DeviceType enum value, or UNKNOWN if invalid
     */
    private DeviceType parseDeviceType(String deviceTypeStr) {
        if (deviceTypeStr == null || deviceTypeStr.isBlank()) {
            return DeviceType.UNKNOWN;
        }

        try {
            return DeviceType.valueOf(deviceTypeStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid device type: {}, defaulting to UNKNOWN", deviceTypeStr);
            return DeviceType.UNKNOWN;
        }
    }

    /**
     * Consumer for dead-letter queue events.
     *
     * This listener monitors the DLQ for failed events that need manual intervention.
     * It logs the failures and increments metrics for alerting.
     *
     * @param eventDto the failed click event from DLQ
     * @param partition the Kafka partition
     * @param offset the message offset
     */
    @KafkaListener(
            topics = "${spring.kafka.topic.click-events.name}-dlq",
            groupId = "click-events-dlq-monitor"
    )
    public void consumeDeadLetterQueue(
            @Payload ClickEventDto eventDto,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.warn("DLQ event received: clickId={}, shortLinkId={}, workspaceId={}, partition={}, offset={}",
                eventDto.getClickId(),
                eventDto.getShortLinkId(),
                eventDto.getWorkspaceId(),
                partition,
                offset);

        // Increment DLQ metric for alerting
        Counter.builder("kafka.click.events.dlq.count")
                .description("Number of click events in dead-letter queue")
                .register(processingTimer.getId().getMeterRegistry())
                .increment();

        // TODO: Consider persisting to a failed_events table for manual review
        // or sending to external monitoring service (Datadog, New Relic, etc.)
    }
}
