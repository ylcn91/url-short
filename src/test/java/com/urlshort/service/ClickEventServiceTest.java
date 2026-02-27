package com.urlshort.service;

import com.urlshort.dto.event.ClickEventDto;
import com.urlshort.event.ClickEventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClickEventService Unit Tests")
class ClickEventServiceTest {

    @Mock
    private ClickEventProducer clickEventProducer;

    @InjectMocks
    private ClickEventService clickEventService;

    @Test
    @DisplayName("recordClickEvent publishes event via ClickEventProducer")
    void recordClickEvent_publishesEvent() {
        doNothing().when(clickEventProducer).publishClickEvent(any(ClickEventDto.class));

        clickEventService.recordClickEvent(
                1L, 1L, "abc123",
                "https://example.com", "192.168.1.1",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                "https://google.com"
        );

        verify(clickEventProducer).publishClickEvent(any(ClickEventDto.class));
    }

    @Test
    @DisplayName("recordClickEvent with null userAgent does not throw")
    void recordClickEvent_nullUserAgent_doesNotThrow() {
        doNothing().when(clickEventProducer).publishClickEvent(any(ClickEventDto.class));

        assertThatCode(() -> clickEventService.recordClickEvent(
                1L, 1L, "abc123",
                "https://example.com", "192.168.1.1",
                null,
                null
        )).doesNotThrowAnyException();

        verify(clickEventProducer).publishClickEvent(any(ClickEventDto.class));
    }

    @Test
    @DisplayName("recordClickEvent failure logs error but does not throw")
    void recordClickEvent_failure_doesNotThrow() {
        doThrow(new RuntimeException("Kafka unavailable"))
                .when(clickEventProducer).publishClickEvent(any(ClickEventDto.class));

        assertThatCode(() -> clickEventService.recordClickEvent(
                1L, 1L, "abc123",
                "https://example.com", "192.168.1.1",
                "Mozilla/5.0", "https://google.com"
        )).doesNotThrowAnyException();
    }
}
