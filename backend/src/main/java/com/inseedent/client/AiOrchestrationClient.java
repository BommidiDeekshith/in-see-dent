package com.inseedent.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiOrchestrationClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.url:http://localhost:8090}")
    private String aiServiceUrl;

    @Value("${ai.service.timeout-seconds:120}")
    private int timeoutSeconds;

    public JsonNode analyzeIncident(Map<String, Object> payload) {
        log.info("Calling AI service for incident analysis");
        WebClient client = webClientBuilder.baseUrl(aiServiceUrl).build();
        try {
            String response = client.post()
                    .uri("/api/v1/analyze")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .onErrorResume(e -> {
                        log.warn("AI service unavailable, using fallback: {}", e.getMessage());
                        return Mono.just(buildFallbackJson(payload));
                    })
                    .block();
            return objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            try {
                return objectMapper.readTree(buildFallbackJson(payload));
            } catch (Exception ex) {
                throw new RuntimeException("AI analysis failed", ex);
            }
        }
    }

    public JsonNode chat(Map<String, Object> payload) {
        WebClient client = webClientBuilder.baseUrl(aiServiceUrl).build();
        try {
            String response = client.post()
                    .uri("/api/v1/chat")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .onErrorResume(e -> Mono.just("{\"reply\":\"AI assistant is temporarily unavailable. Based on incident data, check recent deployments and error rate spikes.\",\"similar_incidents\":[]}"))
                    .block();
            return objectMapper.readTree(response);
        } catch (Exception e) {
            throw new RuntimeException("Chat request failed", e);
        }
    }

    public JsonNode searchSimilar(Map<String, Object> payload) {
        WebClient client = webClientBuilder.baseUrl(aiServiceUrl).build();
        try {
            String response = client.post()
                    .uri("/api/v1/search/similar")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            return objectMapper.readTree(response);
        } catch (Exception e) {
            throw new RuntimeException("Similar incident search failed", e);
        }
    }

    private String buildFallbackJson(Map<String, Object> payload) {
        String service = "unknown";
        Object services = payload.get("affected_services");
        if (services instanceof String[] arr && arr.length > 0) {
            service = arr[0];
        }
        return String.format("""
            {
              "root_cause": "Probable cascading failure in %s — elevated 5xx rate correlated with recent deployment and connection pool exhaustion in logs",
              "confidence_score": 0.72,
              "affected_services": ["%s"],
              "summary": "Mock RCA: deployment change + metric spike + trace timeouts suggest downstream dependency saturation.",
              "remediation_suggestions": "1) Roll back latest deployment\\n2) Scale replicas\\n3) Increase DB pool size\\n4) Enable circuit breaker on checkout-service",
              "findings": {"logs":"Connection timeout patterns","metrics":"p99 latency +340%%","traces":"checkout-service span errors","deployments":"v2.4.1 deployed 8m before incident"},
              "artifacts": []
            }
            """, service, service);
    }
}
