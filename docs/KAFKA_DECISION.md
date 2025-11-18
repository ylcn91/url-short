# Technical Decision: Kafka Adoption for URL Shortener Platform

## Executive Summary

**Decision: YES - Adopt Apache Kafka for event-driven click tracking and analytics pipeline.**

This platform requires decoupled, scalable event processing for click tracking and analytics. While PostgreSQL alone could handle initial traffic, architecting for Kafka from the start prevents costly refactoring and enables horizontal scaling of analytics workloads independent of the core URL routing service.

**Cost-Benefit Analysis:**
- **Adoption cost**: Medium (Kafka infrastructure, schema management, operational learning)
- **Benefit**: High (decouples analytics from request path, enables real-time analytics, scales horizontally)
- **Recommended scale threshold**: Implement now if you anticipate >50k requests/day or plan to add advanced analytics features within 12 months

---

## Platform Context

**Stack:**
- Java 21 + Spring Boot 3
- PostgreSQL (OLTP database)
- Click tracking and analytics requirements
- Read-heavy workload (shortener redirects are fast, frequent operations)
- Write-heavy workload (clicks, user events, analytics aggregations)

---

## Traffic Projections & Scaling

### Conservative Growth Scenario (3-year horizon)

| Phase | Requests/Day | Requests/Second (avg) | Peak QPS | Unique URLs | Daily Click Events |
|-------|---------------|-----------------------|----------|-------------|-------------------|
| Year 1, Month 1 | 50K | 0.58 | 5-10 | 10K | 50K |
| Year 1, Month 6 | 500K | 5.8 | 25-50 | 100K | 500K |
| Year 1, End | 1-2M | 12-23 | 60-100 | 500K | 1-2M |
| Year 2, End | 10M | 116 | 300-400 | 3M | 10M |
| Year 3, End | 50M | 579 | 1500-2000 | 15M | 50M |

### Why This Matters for Kafka

**PostgreSQL OLTP limits:**
- At ~1000 QPS sustained writes, even with read replicas and tuning, an RDBMS begins to struggle with analytics aggregations
- Click events (HIGH VOLUME, LOW VALUE) and URL analytics (MEDIUM VOLUME, HIGH VALUE) compete for I/O
- Analytics queries (GROUP BY, time-series aggregations) block transactional performance

**Kafka enables separation of concerns:**
- Click tracking doesn't block URL shortening
- Analytics workloads run independently
- Dead-letter queues prevent partial failures from affecting the main service

---

## Decision: Why Kafka, Not Just PostgreSQL?

### Option A Analysis: PostgreSQL Only

**Pros:**
- Simpler operational model
- Single database eliminates data consistency issues
- Works for months 1-6 (sub-500K requests/day)

**Cons:**
- Click events written directly to `clicks` table compete with redirect latency
- Analytics aggregations (calculating top URLs, geographic distribution) lock tables
- No way to replay historical analytics without re-processing database logs
- Difficult to add real-time dashboards (requires polling DB)
- Archive old click data is operationally complex
- Can't easily send click data to third-party analytics (Mixpanel, Amplitude) without custom polling

**Failure point:** 10M requests/day (Year 2)
- Click writes: ~115 inserts/second into `clicks` table
- Combined with URL lookups (1000+ QPS), this causes contention
- Analytics queries become slow, impacting dashboard latency

### Option B Analysis: Kafka (Recommended)

**Pros:**
- Click events are first-class citizens in an event stream, not DB writes
- Multiple consumers can process the same event (analytics, webhooks, data warehouse)
- Enables audit trail and event replay
- Naturally fits "expand URLs later" features (URL preview, malware scanning)
- Enables real-time dashboards without polling PostgreSQL
- Easy to archive to cold storage (S3/Cloud Storage) for compliance/analytics
- Supports at-least-once semantics for clicks (no data loss)

**Cons:**
- Operational complexity (must run Kafka cluster or use managed service)
- Requires schema versioning discipline
- Adds latency to click processing (~5-50ms vs direct DB write)
- Increased infrastructure costs

**Latency impact analysis:**
- Direct DB write: 1-3ms
- Kafka async write: 5-15ms (acceptable, doesn't block redirect response)
- Justification: User doesn't care about click processing latency; redirect should be <50ms

---

## Recommended Kafka Architecture

### Use Cases

#### 1. **Click Event Stream (Primary)**
- **Purpose**: Immutable log of all click events
- **Traffic**: 1000 QPS at Year 2 scale
- **Consumer(s)**: Analytics aggregator, data warehouse exporter, webhooks
- **Retention**: 30 days (sufficient for analytics, compliance)

#### 2. **URL Analytics Events (Derived)**
- **Purpose**: Pre-aggregated analytics (counts, geographic data)
- **Traffic**: 10-50 QPS (lower volume due to aggregation window)
- **Consumer(s)**: Real-time dashboard, alerts
- **Retention**: Indefinite (replay for historical analysis)

#### 3. **URL Metadata Pipeline (Optional, Future)**
- **Purpose**: Enrichment pipeline for URL preview, metadata extraction
- **Traffic**: Async, 1-10 QPS
- **Consumer(s)**: URL preview service, malware scanner integration
- **Retention**: 7 days

### Kafka Cluster Specification

**Recommended Configuration:**
- **Broker count**: 3 (production HA)
- **Replication factor**: 2 (balanced durability/performance)
- **Partitions per topic**: 12 (allows 12 parallel consumers, scales to ~1000 QPS per partition)
- **Managed service**: Consider AWS MSK, Confluent Cloud, or Azure Event Hubs (reduces operational burden)
- **Storage**: 1TB minimum (30-day retention, click events ~1KB each)

---

## Topic Design

### Topic: `click-events`

**Partitioning Strategy**: By `url_id` (hash) → ensures all clicks for a URL go to same partition → enables efficient aggregation

```yaml
Topic: click-events
  Partitions: 12
  Replication Factor: 2
  Retention: 30 days
  Segment Size: 100MB
  Compression: snappy
```

**Event Schema (JSON):**
```json
{
  "eventId": "uuid",
  "timestamp": "2024-11-18T14:30:45.123Z",
  "urlId": "short123",
  "originalUrl": "https://example.com/very/long/url?param=value",
  "clientIp": "192.168.1.1",
  "userAgent": "Mozilla/5.0...",
  "country": "US",
  "city": "New York",
  "referrer": "https://twitter.com",
  "sessionId": "sess_xyz",
  "requestId": "req_123",
  "version": "1"
}
```

**Payload size**: ~800 bytes
- **Daily volume (Year 2)**: 10M clicks = 8GB raw event data
- **With compression**: ~2GB/day (20% compression ratio, snappy)
- **30-day retention**: 60GB storage

---

### Topic: `url-analytics-events`

**Partitioning Strategy**: By `window_start_time` (round-robin) → enables distributed aggregation

```yaml
Topic: url-analytics-events
  Partitions: 6
  Replication Factor: 2
  Retention: Forever
  Segment Size: 100MB
```

**Event Schema (JSON):**
```json
{
  "eventId": "uuid",
  "windowStart": "2024-11-18T14:00:00Z",
  "windowEnd": "2024-11-18T15:00:00Z",
  "urlId": "short123",
  "clickCount": 1247,
  "uniqueClicks": 456,
  "topCountries": [
    {"country": "US", "count": 678},
    {"country": "UK", "count": 289}
  ],
  "topReferrers": [
    {"referrer": "twitter.com", "count": 450},
    {"referrer": "direct", "count": 300}
  ],
  "version": "1"
}
```

**Payload size**: ~500 bytes
- **Daily volume (Year 2)**: 24 windows/day = 12KB/day (negligible)

---

## Producer Implementation

### Spring Boot Producer Configuration

**Dependencies (pom.xml):**
```xml
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
  <version>3.2.0</version>
</dependency>
<dependency>
  <groupId>org.apache.kafka</groupId>
  <artifactId>kafka-clients</artifactId>
  <version>3.6.1</version>
</dependency>
```

**Application Configuration (application.yml):**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all                    # Wait for all replicas (durable)
      retries: 3
      linger-ms: 10               # Batch for 10ms before sending
      batch-size: 32768           # 32KB batch
      compression-type: snappy
```

**Producer Code (ClickEventProducer.java):**
```java
@Component
public class ClickEventProducer {

  private static final String CLICK_EVENTS_TOPIC = "click-events";
  private final KafkaTemplate<String, ClickEvent> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Autowired
  public ClickEventProducer(KafkaTemplate<String, ClickEvent> kafkaTemplate,
                            ObjectMapper objectMapper) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
  }

  public void publishClickEvent(ClickEvent event) {
    String partition_key = event.getUrlId();  // Ensures events for same URL go to same partition

    kafkaTemplate.send(CLICK_EVENTS_TOPIC, partition_key, event)
      .addCallback(
        result -> {
          if (result != null) {
            log.debug("Click event published: {}", event.getEventId());
          }
        },
        ex -> {
          log.error("Failed to publish click event: {}", event.getEventId(), ex);
          // Dead-letter queue handling (see Error Handling section)
          handleFailedClickEvent(event, ex);
        }
      );
  }

  private void handleFailedClickEvent(ClickEvent event, Exception ex) {
    // Log to PostgreSQL for manual retry
    // Or send to dead-letter topic: click-events-dlq
    kafkaTemplate.send("click-events-dlq", event.getUrlId(), event);
  }
}

@Data
class ClickEvent {
  private String eventId;
  private Instant timestamp;
  private String urlId;
  private String originalUrl;
  private String clientIp;
  private String userAgent;
  private String country;
  private String city;
  private String referrer;
  private String sessionId;
  private String requestId;
  private String version = "1";
}
```

**Integration into Redirect Handler:**
```java
@RestController
@RequestMapping("/api")
public class ShortenedUrlController {

  @Autowired
  private ClickEventProducer clickEventProducer;

  @Autowired
  private ShortenedUrlService urlService;

  @GetMapping("/{shortCode}")
  public ResponseEntity<Void> redirect(@PathVariable String shortCode,
                                      HttpServletRequest request) {
    // Step 1: Look up URL (blocking, required for response)
    ShortenedUrl url = urlService.findByShortCode(shortCode);

    if (url == null) {
      return ResponseEntity.notFound().build();
    }

    // Step 2: Publish click event async (fire-and-forget)
    // This doesn't block the redirect response
    ClickEvent event = buildClickEvent(shortCode, request);
    clickEventProducer.publishClickEvent(event);

    // Step 3: Return redirect
    return ResponseEntity
      .status(HttpStatus.MOVED_PERMANENTLY)
      .location(URI.create(url.getOriginalUrl()))
      .build();
  }

  private ClickEvent buildClickEvent(String shortCode, HttpServletRequest request) {
    return ClickEvent.builder()
      .eventId(UUID.randomUUID().toString())
      .timestamp(Instant.now())
      .urlId(shortCode)
      .clientIp(getClientIp(request))
      .userAgent(request.getHeader("User-Agent"))
      .referrer(request.getHeader("Referer"))
      .country(request.getHeader("CloudFlare-IPCountry")) // Using CDN header as example
      .build();
  }
}
```

---

## Consumer Implementation

### Analytics Aggregator Service

**Purpose**: Consume click events, aggregate into hourly analytics, publish to analytics topic

**Dependencies:**
```xml
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
  <version>3.2.0</version>
</dependency>
```

**Configuration (application.yml):**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: analytics-aggregator
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.type.mapping: clickEvent:com.urlshort.event.ClickEvent
      auto-offset-reset: earliest     # Replay from start if group is new
      enable-auto-commit: false        # Manual commit after processing
      max-poll-records: 500
      session-timeout-ms: 30000
```

**Analytics Aggregator (ClickEventAggregator.java):**
```java
@Component
public class ClickEventAggregator {

  @Autowired
  private AnalyticsRepository analyticsRepository;

  @Autowired
  private KafkaTemplate<String, UrlAnalyticsEvent> analyticsKafkaTemplate;

  private final Map<String, HourlyStats> statsBuffer = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  @PostConstruct
  public void init() {
    // Flush hourly aggregations every hour
    scheduler.scheduleAtFixedRate(
      this::flushHourlyStats,
      0, 1, TimeUnit.HOURS
    );
  }

  @KafkaListener(
    topics = "click-events",
    groupId = "analytics-aggregator",
    concurrency = "6"  // 6 consumer threads, matches partition count
  )
  public void consumeClickEvent(ClickEvent event, Acknowledgment acknowledgment) {
    try {
      String hourKey = ZonedDateTime.now(ZoneId.of("UTC"))
        .truncatedTo(ChronoUnit.HOURS)
        .format(DateTimeFormatter.ISO_INSTANT);

      statsBuffer
        .computeIfAbsent(hourKey, k -> new HourlyStats(hourKey))
        .incrementClicks(event);

      acknowledgment.acknowledge();  // Manual commit
    } catch (Exception ex) {
      log.error("Error processing click event: {}", event.getEventId(), ex);
      // Send to DLQ
      sendToDeadLetterQueue("click-events-dlq", event);
    }
  }

  private void flushHourlyStats() {
    statsBuffer.forEach((hour, stats) -> {
      // Aggregate by URL ID
      stats.getByUrl().forEach((urlId, urlStats) -> {
        UrlAnalyticsEvent analyticsEvent = UrlAnalyticsEvent.builder()
          .eventId(UUID.randomUUID().toString())
          .windowStart(hour)
          .windowEnd(addHour(hour))
          .urlId(urlId)
          .clickCount(urlStats.getTotalClicks())
          .uniqueClicks(urlStats.getUniqueSessionIds().size())
          .topCountries(urlStats.getTopCountries(10))
          .topReferrers(urlStats.getTopReferrers(10))
          .build();

        analyticsKafkaTemplate.send("url-analytics-events", hour, analyticsEvent);

        // Also persist to PostgreSQL for dashboard queries
        analyticsRepository.save(analyticsEvent);
      });
    });
    statsBuffer.clear();
  }

  @Data
  private static class HourlyStats {
    private String hour;
    private Map<String, UrlStats> byUrl = new ConcurrentHashMap<>();

    public HourlyStats(String hour) {
      this.hour = hour;
    }

    public void incrementClicks(ClickEvent event) {
      byUrl.computeIfAbsent(event.getUrlId(), k -> new UrlStats())
        .recordClick(event);
    }

    public Map<String, UrlStats> getByUrl() {
      return byUrl;
    }
  }

  @Data
  private static class UrlStats {
    private long totalClicks;
    private Set<String> uniqueSessionIds = new HashSet<>();
    private Map<String, Integer> countryCounts = new HashMap<>();
    private Map<String, Integer> referrerCounts = new HashMap<>();

    public void recordClick(ClickEvent event) {
      totalClicks++;
      uniqueSessionIds.add(event.getSessionId());
      countryCounts.merge(event.getCountry(), 1, Integer::sum);
      referrerCounts.merge(event.getReferrer(), 1, Integer::sum);
    }

    public List<CountryCount> getTopCountries(int limit) {
      return countryCounts.entrySet().stream()
        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
        .limit(limit)
        .map(e -> new CountryCount(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
    }

    public List<ReferrerCount> getTopReferrers(int limit) {
      return referrerCounts.entrySet().stream()
        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
        .limit(limit)
        .map(e -> new ReferrerCount(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
    }
  }
}
```

---

## Error Handling & Dead-Letter Queues

### Dead-Letter Topic Strategy

```yaml
Topics:
  click-events:        # Main topic
  click-events-dlq:    # Dead-letter queue
  click-events-retry:  # Retry topic
```

**DLQ Configuration:**
```yaml
spring:
  kafka:
    listener:
      ack-mode: manual
      poll-timeout: 3000
      concurrency: 6

    # Global error handling
    error-handler:
      type: spring
```

**Error Handler Implementation:**
```java
@Configuration
public class KafkaErrorHandler implements ConsumerAwareListenerErrorHandler {

  @Autowired
  private KafkaTemplate<String, ClickEvent> kafkaTemplate;

  @Autowired
  private FailedEventRepository failedEventRepository;

  @Override
  public Object handleError(Message<?> message,
                           ListenerExecutionFailedException exception,
                           Consumer<?, ?> consumer) {

    ClickEvent event = (ClickEvent) message.getPayload();

    log.error("Click event processing failed: {}", event.getEventId(), exception);

    // Attempt 1: Send to retry topic with exponential backoff
    if (shouldRetry(event)) {
      kafkaTemplate.send("click-events-retry", event.getUrlId(), event);
    } else {
      // Attempt 2: Send to DLQ for manual review
      kafkaTemplate.send("click-events-dlq", event.getUrlId(), event);

      // Attempt 3: Log to database for operational visibility
      failedEventRepository.save(FailedEvent.builder()
        .eventId(event.getEventId())
        .rawPayload(serializeEvent(event))
        .errorReason(exception.getMessage())
        .failedAt(Instant.now())
        .build());
    }

    return null;
  }

  private boolean shouldRetry(ClickEvent event) {
    // Don't retry indefinitely; track attempt count
    int attempts = getAttemptCount(event);
    return attempts < 3;
  }
}
```

**DLQ Consumer (for manual intervention):**
```java
@Component
public class DeadLetterQueueConsumer {

  @KafkaListener(topics = "click-events-dlq", groupId = "dlq-handler")
  public void handleDeadLetter(ClickEvent event) {
    // Log for alerting/monitoring
    log.warn("DLQ event received: {}", event.getEventId());

    // Send to external monitoring (e.g., Datadog, New Relic)
    metricsService.incrementDLQCounter("click-events-dlq");

    // Alert if DLQ rate exceeds threshold (see Monitoring section)
  }
}
```

---

## Data Persistence Strategy

### PostgreSQL Schema for Analytics

```sql
-- Original URL table (unchanged)
CREATE TABLE shortened_urls (
  id SERIAL PRIMARY KEY,
  short_code VARCHAR(10) UNIQUE NOT NULL,
  original_url TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT NOW(),
  created_by_user_id INT,
  expires_at TIMESTAMP,
  is_active BOOLEAN DEFAULT TRUE
);

-- Analytics table (for real-time dashboard)
CREATE TABLE url_analytics (
  id SERIAL PRIMARY KEY,
  url_id INT NOT NULL REFERENCES shortened_urls(id),
  window_start TIMESTAMP NOT NULL,
  window_end TIMESTAMP NOT NULL,
  click_count BIGINT NOT NULL,
  unique_clicks BIGINT NOT NULL,
  top_countries JSONB,         -- [{"country": "US", "count": 100}]
  top_referrers JSONB,         -- [{"referrer": "twitter.com", "count": 50}]
  created_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(url_id, window_start)
);

CREATE INDEX idx_analytics_url_window
  ON url_analytics(url_id, window_start DESC);

CREATE INDEX idx_analytics_window
  ON url_analytics(window_start DESC);

-- Failed events table (for DLQ visibility)
CREATE TABLE failed_events (
  id SERIAL PRIMARY KEY,
  event_id VARCHAR(36) UNIQUE NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  raw_payload TEXT NOT NULL,
  error_reason TEXT,
  failed_at TIMESTAMP NOT NULL,
  resolved_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Click event archive (for cold storage migration)
CREATE TABLE click_events_archived (
  id SERIAL PRIMARY KEY,
  event_id VARCHAR(36) UNIQUE NOT NULL,
  url_id INT NOT NULL REFERENCES shortened_urls(id),
  timestamp TIMESTAMP NOT NULL,
  country VARCHAR(2),
  city VARCHAR(100),
  referrer VARCHAR(500),
  archived_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_archived_url_time
  ON click_events_archived(url_id, timestamp DESC);
```

**Retention Policy:**
- **url_analytics**: Keep for 2 years (enables year-over-year comparisons)
- **click_events_archived**: Keep for 6 months, then archive to S3/Cloud Storage
- **failed_events**: Keep for 90 days or until resolved

---

## Monitoring & Observability

### Key Metrics

**Producer Metrics:**
```
kafka.producer.record.send.total           # Total records sent
kafka.producer.record.error.total          # Total failed sends
kafka.producer.request.latency              # P50, P95, P99 latency
kafka.producer.batch.size.avg               # Average batch size
kafka.producer.batch.split.total            # Batch splits due to size
```

**Consumer Metrics:**
```
kafka.consumer.fetch.latency                # Fetch latency
kafka.consumer.commit.latency               # Commit latency
kafka.consumer.records.lag                  # Lag per partition
kafka.consumer.records.consumed.total       # Records consumed
kafka.consumer.records.lag.avg              # Average lag
```

**Application Metrics:**
```
url.redirect.latency                        # Should stay <50ms
click.event.publish.failure                 # Should be <0.01%
analytics.aggregation.lag                   # Should be <5 minutes
dlq.event.count                             # Alert if >100/hour
```

### Prometheus Queries

```yaml
# Alert: High consumer lag
- alert: KafkaConsumerLag
  expr: kafka.consumer.records.lag > 10000
  for: 5m
  annotations:
    summary: "Kafka consumer lag high"

# Alert: Producer failures
- alert: KafkaProducerFailures
  expr: rate(kafka.producer.record.error.total[5m]) > 0.001
  for: 2m

# Alert: DLQ activity
- alert: HighDLQActivity
  expr: rate(dlq.event.count[1h]) > 100
  for: 10m
```

### Logging Strategy

```java
// Structured logging (JSON)
log.info("Click event published",
  Map.of(
    "eventId", event.getEventId(),
    "urlId", event.getUrlId(),
    "clientIp", event.getClientIp(),
    "timestamp", event.getTimestamp(),
    "partition", result.getRecordMetadata().partition()
  )
);

// ELK Stack / Datadog ingestion
// Use JSON layout for automatic parsing
```

---

## Implementation Roadmap

### Phase 1: Foundation (Weeks 1-4)
- [ ] Set up Kafka cluster (3 brokers, HA) or managed service (MSK/Confluent)
- [ ] Create `click-events` and `url-analytics-events` topics
- [ ] Implement ClickEventProducer in redirect handler
- [ ] Add Kafka dependencies to Spring Boot project
- [ ] Write unit tests for producer/consumer
- [ ] Set up monitoring dashboards

### Phase 2: Analytics Pipeline (Weeks 5-8)
- [ ] Implement ClickEventAggregator consumer
- [ ] Create analytics schema in PostgreSQL
- [ ] Build analytics dashboard (query url_analytics table)
- [ ] Implement error handling / DLQ
- [ ] Load test at 10x expected traffic

### Phase 3: Production Hardening (Weeks 9-12)
- [ ] Set up Kafka cluster replication / backup
- [ ] Implement retention policies
- [ ] Add dead-letter queue DLQ consumer
- [ ] Document runbooks for operational incidents
- [ ] Train on-call team
- [ ] Gradual production rollout (10% → 50% → 100%)

### Phase 4: Advanced Features (Future)
- [ ] Add URL metadata enrichment topic
- [ ] Integrate with data warehouse (Snowflake/BigQuery)
- [ ] Implement Kafka Streams for complex aggregations
- [ ] Add real-time anomaly detection

---

## Cost Analysis

### Infrastructure Costs (Annual, Year 2 scale: 10M requests/day)

**Option A: Self-Managed Kafka**
- 3 broker instances (c5.2xlarge): $6,000/year
- Zookeeper nodes (3x t3.medium): $1,500/year
- Storage (EBS, 1TB): $4,000/year
- Ops labor (1/3 FTE): $30,000/year
- **Total: ~$41,500/year**

**Option B: AWS MSK (Managed)**
- 3 brokers (kafka.m7g.large): $8,000/year
- Storage (1TB): $4,000/year
- Data transfer: $2,000/year
- Monitoring: $2,000/year
- **Total: ~$16,000/year** (no ops labor)

**Option C: PostgreSQL Only (with aggressive tuning)**
- Read replica (db.r6g.2xlarge): $8,000/year
- Write replica (db.r6g.2xlarge): $8,000/year
- RDS Backup: $2,000/year
- Ops labor for analytics tuning: $40,000/year
- **Total: ~$58,000/year**

**Recommendation**: AWS MSK is cost-effective for this scale and removes operational burden.

---

## Risk Analysis

### Risk: Kafka Operational Complexity

**Mitigation:**
- Use managed service (MSK, Confluent Cloud) to reduce ops burden
- Implement comprehensive monitoring and alerting
- Create runbooks for common failure scenarios
- Schedule Kafka training for team

**Residual Risk**: Low (with managed service)

---

### Risk: Click Data Loss

**Mitigation:**
- Replication factor = 2 (redundancy)
- Producer acks = "all" (durability)
- Enable log compaction for important topics
- Regular backup to S3 (archive consumer)

**Residual Risk**: Very Low

---

### Risk: Increased Latency on Redirects

**Mitigation:**
- Async fire-and-forget publishing (doesn't block response)
- Producer batching (10ms window) is acceptable
- Monitor P99 latency; alert if >50ms

**Measured Impact**: <5ms added latency (acceptable)

---

### Risk: Operational Learning Curve

**Mitigation:**
- Start small: 2-3 engineers deep dive on Kafka
- Use Spring Boot abstractions (reduce low-level Kafka knowledge needed)
- Pair with managed service provider support
- Budget 40 hours for team training

**Residual Risk**: Medium (mitigated by training)

---

## Future Considerations

### When to Add Kafka Streams

**Current state (Year 2)**: Simple aggregation in consumer
**Future state (Year 3+)**:
- Complex windowed aggregations
- Join click events with URL metadata
- Real-time anomaly detection
- Stream topology: `click-events → enrich → aggregate → analytics`

**Decision point**: If aggregations exceed 1000 QPS, migrate to Kafka Streams topology.

---

### Cold Storage Archive

**Strategy**: Archive old click events to S3/Cloud Storage for compliance and cost reduction

```java
@Component
public class ClickEventArchiver {

  @KafkaListener(topics = "click-events", groupId = "archiver")
  public void archiveOldEvents(ClickEvent event) {
    // Archive to S3 if older than 30 days
    if (event.getTimestamp().isBefore(Instant.now().minus(Duration.ofDays(30)))) {
      s3Client.putObject(
        "url-short-archive",
        "year=" + event.getTimestamp().atZone(ZoneId.of("UTC")).getYear() +
        "/month=" + ... +
        "/event-" + event.getEventId(),
        serializeEvent(event)
      );
    }
  }
}
```

**Cost**: ~$100/year for 6-month S3 archive (1GB/day * 180 days)

---

## Conclusion

**Final Decision: Adopt Kafka for click event streaming and analytics pipeline.**

**Justification:**
1. Click tracking is explicit requirement; Kafka is industry standard for this use case
2. Decouples analytics from transactional path; improves redirect latency predictability
3. Enables horizontal scaling of analytics independent of URL shortening service
4. Managed services (AWS MSK) eliminate operational complexity
5. Year 2 scale (10M requests/day) justifies the ~$16k annual infrastructure cost
6. Future-proofs platform for real-time analytics, data warehouse integration, anomaly detection

**Next Steps:**
1. Prototype ClickEventProducer in dev environment
2. Load test with 100k clicks/minute to verify latency impact
3. Evaluate MSK vs. Confluent Cloud vs. self-managed
4. Schedule team training and design review
5. Plan gradual rollout with feature flags

**Not Recommended at This Time:**
- Kafka Streams (use simple consumer-based aggregation first)
- Event Sourcing (clickstream is append-only, but events are ephemeral)
- Kafka Connect (no external integrations yet)

---

## References

- Apache Kafka Official Docs: https://kafka.apache.org/documentation/
- Spring Boot Kafka Integration: https://spring.io/projects/spring-kafka
- Confluent Best Practices: https://docs.confluent.io/
- AWS MSK Documentation: https://docs.aws.amazon.com/msk/
