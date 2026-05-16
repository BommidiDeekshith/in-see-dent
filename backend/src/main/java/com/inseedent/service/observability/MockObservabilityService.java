package com.inseedent.service.observability;

import com.inseedent.domain.*;
import com.inseedent.exception.ResourceNotFoundException;
import com.inseedent.repository.*;
import com.inseedent.service.incident.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockObservabilityService {

    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;
    private final LogRepository logRepository;
    private final MetricRepository metricRepository;
    private final TraceRepository traceRepository;
    private final DeploymentRepository deploymentRepository;
    private final TimelineService timelineService;
    private final Random random = new Random(42);

    @Transactional
    public Map<String, Integer> ingestMockTelemetry(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentId));

        String service = incident.getAffectedServices() != null && incident.getAffectedServices().length > 0
                ? incident.getAffectedServices()[0] : "checkout-service";

        LocalDateTime base = incident.getStartTime() != null ? incident.getStartTime() : LocalDateTime.now();
        int alerts = ingestAlerts(incident, service, base);
        int logs = ingestLogs(incident, service, base);
        int metrics = ingestMetrics(incident, service, base);
        int traces = ingestTraces(incident, service, base);
        int deployments = ingestDeployments(incident, service, base);

        return Map.of("alerts", alerts, "logs", logs, "metrics", metrics, "traces", traces, "deployments", deployments);
    }

    private int ingestAlerts(Incident incident, String service, LocalDateTime base) {
        String[][] samples = {
                {"Grafana", "HighErrorRate", "critical", "5xx rate > 5% on " + service},
                {"Prometheus", "LatencyP99Spike", "high", "p99 latency exceeded 2s"},
                {"Grafana", "PodRestartLoop", "high", "Pods restarting in " + service}
        };
        for (String[] s : samples) {
            Alert alert = Alert.builder()
                    .incident(incident)
                    .alertSource(s[0])
                    .alertName(s[1])
                    .severity(s[2])
                    .description(s[3])
                    .triggeredAt(base.minusMinutes(random.nextInt(15)))
                    .build();
            alertRepository.save(alert);
            timelineService.addTimelineEvent(incident.getId(), "alert", s[3], s[2], s[0], null);
        }
        return samples.length;
    }

    private int ingestLogs(Incident incident, String service, LocalDateTime base) {
        String[] messages = {
                "ERROR connection pool exhausted - HikariPool",
                "WARN downstream payment-gateway timeout after 30000ms",
                "ERROR java.net.SocketTimeoutException: Read timed out",
                "ERROR checkout-service: circuit breaker OPEN"
        };
        for (String msg : messages) {
            logRepository.save(Log.builder()
                    .incident(incident)
                    .serviceName(service)
                    .logLevel(msg.startsWith("ERROR") ? "ERROR" : "WARN")
                    .message(msg)
                    .logSource("Loki")
                    .timestamp(base.minusMinutes(random.nextInt(20)))
                    .build());
        }
        return messages.length;
    }

    private int ingestMetrics(Incident incident, String service, LocalDateTime base) {
        String[] names = {"http_requests_total", "http_request_duration_seconds", "jvm_memory_used_bytes"};
        for (String name : names) {
            metricRepository.save(Metric.builder()
                    .incident(incident)
                    .metricName(name)
                    .serviceName(service)
                    .metricType("gauge")
                    .metricValue(100 + random.nextDouble() * 500)
                    .timestamp(base.minusMinutes(random.nextInt(10)))
                    .build());
        }
        timelineService.addTimelineEvent(incident.getId(), "metric_spike",
                "Metric spike detected on " + service, "high", "Prometheus", null);
        return names.length;
    }

    private int ingestTraces(Incident incident, String service, LocalDateTime base) {
        traceRepository.save(Trace.builder()
                .incident(incident)
                .traceId(UUID.randomUUID().toString().replace("-", ""))
                .serviceName(service)
                .spanName("POST /api/checkout")
                .durationMs(4500L)
                .status("ERROR")
                .errorMessage("Upstream dependency timeout")
                .timestamp(base.minusMinutes(5))
                .build());
        timelineService.addTimelineEvent(incident.getId(), "trace_error",
                "Trace failures in " + service, "high", "OpenTelemetry", null);
        return 1;
    }

    private int ingestDeployments(Incident incident, String service, LocalDateTime base) {
        deploymentRepository.save(Deployment.builder()
                .incident(incident)
                .serviceName(service)
                .version("v2.4.1")
                .previousVersion("v2.4.0")
                .deployedBy("ci-pipeline")
                .deploymentTime(base.minusMinutes(12))
                .rollbackAvailable(true)
                .build());
        timelineService.addTimelineEvent(incident.getId(), "deployment",
                "Deployed " + service + " v2.4.1", "medium", "argocd", null);
        return 1;
    }
}
