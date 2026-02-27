package com.urlshort.service;

import com.urlshort.domain.DeviceType;
import com.urlshort.dto.event.ClickEventDto;
import com.urlshort.event.ClickEventProducer;
import com.urlshort.util.UserAgentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClickEventService {

    private final ClickEventProducer clickEventProducer;

    @Async
    public void recordClickEvent(Long shortLinkId, Long workspaceId, String shortCode,
                                  String originalUrl, String ipAddress, String userAgent,
                                  String referrer) {
        try {
            UserAgentParser.ParseResult uaInfo = UserAgentParser.parse(userAgent);

            ClickEventDto clickEvent = ClickEventDto.builder()
                    .clickId(UUID.randomUUID().toString())
                    .shortLinkId(shortLinkId)
                    .workspaceId(workspaceId)
                    .timestamp(Instant.now())
                    .ip(ipAddress)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .country(null)
                    .city(null)
                    .deviceType(mapDeviceType(uaInfo.deviceType()))
                    .browser(uaInfo.browser())
                    .os(uaInfo.os())
                    .originalUrl(originalUrl)
                    .shortCode(shortCode)
                    .version("1")
                    .build();

            clickEventProducer.publishClickEvent(clickEvent);
            log.debug("Click event published for linkId={}", shortLinkId);

        } catch (Exception e) {
            log.error("Failed to publish click event for linkId={}: {}", shortLinkId, e.getMessage(), e);
        }
    }

    private String mapDeviceType(DeviceType deviceType) {
        return switch (deviceType) {
            case DESKTOP -> "desktop";
            case MOBILE -> "mobile";
            case TABLET -> "tablet";
            case BOT -> "bot";
            case UNKNOWN -> "unknown";
        };
    }
}
