package com.inseedent.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inseedent.service.analysis.AnalysisService;
import com.inseedent.service.observability.MockObservabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class IncidentEventConsumer {

    private final ObjectMapper objectMapper;
    private final MockObservabilityService mockObservabilityService;
    private final AnalysisService analysisService;

    @KafkaListener(topics = "${kafka.topics.incidents:inseedent.incidents}", groupId = "inseedent-analysis")
    public void onIncidentEvent(String message) {
        try {
            IncidentEvent event = objectMapper.readValue(message, IncidentEvent.class);
            if (!"INCIDENT_CREATED".equals(event.getEventType())) {
                return;
            }
            log.info("Kafka: processing incident created event for id={}", event.getIncidentId());
            mockObservabilityService.ingestMockTelemetry(event.getIncidentId());
            analysisService.triggerAnalysis(event.getIncidentId());
        } catch (Exception e) {
            log.error("Failed to process incident event", e);
        }
    }
}
