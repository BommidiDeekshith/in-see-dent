package com.inseedent.controller;

import com.inseedent.domain.Alert;
import com.inseedent.exception.ResourceNotFoundException;
import com.inseedent.repository.AlertRepository;
import com.inseedent.repository.IncidentRepository;
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
@RequestMapping("/api/v1/incidents/{incidentId}/alerts")
public class AlertController {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private TimelineService timelineService;

    /**
     * List all alerts for an incident
     * GET /api/v1/incidents/{incidentId}/alerts
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Alert>>> listAlerts(@PathVariable Long incidentId) {
        log.info("GET /api/v1/incidents/{}/alerts - Listing alerts", incidentId);

        try {
            if (!incidentRepository.existsById(incidentId)) {
                throw new ResourceNotFoundException("Incident not found with id: " + incidentId);
            }

            List<Alert> alerts = alertRepository.findByIncidentId(incidentId);
            return ResponseEntity.ok(ApiResponse.success(alerts));
        } catch (ResourceNotFoundException e) {
            log.warn("Incident not found: {}", incidentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error listing alerts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error listing alerts: " + e.getMessage()));
        }
    }

    /**
     * Ingest a new alert for an incident
     * POST /api/v1/incidents/{incidentId}/alerts
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Alert>> createAlert(
            @PathVariable Long incidentId,
            @Valid @RequestBody Alert alert) {
        
        log.info("POST /api/v1/incidents/{}/alerts - Creating alert: {}", incidentId, alert.getAlertName());

        try {
            if (!incidentRepository.existsById(incidentId)) {
                throw new ResourceNotFoundException("Incident not found with id: " + incidentId);
            }

            // Validate alert
            if (alert.getAlertName() == null || alert.getAlertName().trim().isEmpty()) {
                throw new IllegalArgumentException("Alert name cannot be empty");
            }
            if (alert.getAlertSource() == null || alert.getAlertSource().trim().isEmpty()) {
                throw new IllegalArgumentException("Alert source cannot be empty");
            }

            // Set incident and triggered time if not provided
            alert.setIncident(incidentRepository.findById(incidentId).get());
            if (alert.getTriggeredAt() == null) {
                alert.setTriggeredAt(LocalDateTime.now());
            }

            Alert savedAlert = alertRepository.save(alert);

            // Auto-add timeline event
            String description = String.format("Alert triggered: %s from %s", alert.getAlertName(), alert.getAlertSource());
            timelineService.addTimelineEvent(incidentId, "alert", description, 
                    alert.getSeverity(), alert.getAlertSource(), alert.getAlertData());

            log.info("Alert created successfully for incident {}: id={}", incidentId, savedAlert.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(savedAlert));
        } catch (ResourceNotFoundException e) {
            log.warn("Incident not found: {}", incidentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid alert input: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating alert", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error creating alert: " + e.getMessage()));
        }
    }
}
