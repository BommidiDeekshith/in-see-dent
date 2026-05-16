package com.inseedent.controller;

import com.inseedent.domain.Metric;
import com.inseedent.exception.ResourceNotFoundException;
import com.inseedent.repository.IncidentRepository;
import com.inseedent.repository.MetricRepository;
import com.inseedent.service.incident.TimelineService;
import com.inseedent.util.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/incidents/{incidentId}/metrics")
public class MetricsController {

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private TimelineService timelineService;

    private static final Double METRIC_SPIKE_THRESHOLD = 90.0;

    /**
     * List all metrics for an incident
     * GET /api/v1/incidents/{incidentId}/metrics
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Metric>>> listMetrics(@PathVariable Long incidentId) {
        log.info("GET /api/v1/incidents/{}/metrics - Listing metrics", incidentId);

        try {
            if (!incidentRepository.existsById(incidentId)) {
                throw new ResourceNotFoundException("Incident not found with id: " + incidentId);
            }

            List<Metric> metrics = metricRepository.findByIncidentId(incidentId);
            return ResponseEntity.ok(ApiResponse.success(metrics));
        } catch (ResourceNotFoundException e) {
            log.warn("Incident not found: {}", incidentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error listing metrics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error listing metrics: " + e.getMessage()));
        }
    }

    /**
     * Ingest a new metric for an incident
     * POST /api/v1/incidents/{incidentId}/metrics
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Metric>> createMetric(
            @PathVariable Long incidentId,
            @Valid @RequestBody Metric metric) {
        
        log.info("POST /api/v1/incidents/{}/metrics - Creating metric: {}", incidentId, metric.getMetricName());

        try {
            if (!incidentRepository.existsById(incidentId)) {
                throw new ResourceNotFoundException("Incident not found with id: " + incidentId);
            }

            // Validate metric
            if (metric.getMetricName() == null || metric.getMetricName().trim().isEmpty()) {
                throw new IllegalArgumentException("Metric name cannot be empty");
            }

            // Set incident and timestamp if not provided
            metric.setIncident(incidentRepository.findById(incidentId).get());
            if (metric.getTimestamp() == null) {
                metric.setTimestamp(LocalDateTime.now());
            }

            Metric savedMetric = metricRepository.save(metric);

            // Auto-add timeline event for anomalies (e.g., value > threshold)
            if (savedMetric.getValue() != null && savedMetric.getValue() > METRIC_SPIKE_THRESHOLD) {
                String description = String.format("Metric anomaly detected: %s = %.2f (threshold: %.2f)", 
                        savedMetric.getMetricName(), savedMetric.getValue(), METRIC_SPIKE_THRESHOLD);
                timelineService.addTimelineEvent(incidentId, "metric_spike", description, 
                        "high", savedMetric.getServiceName(), savedMetric.getLabels());
            }

            log.info("Metric created successfully for incident {}: id={}", incidentId, savedMetric.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(savedMetric));
        } catch (ResourceNotFoundException e) {
            log.warn("Incident not found: {}", incidentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid metric input: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating metric", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error creating metric: " + e.getMessage()));
        }
    }
}
