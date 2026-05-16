package com.inseedent.controller;

import com.inseedent.domain.Deployment;
import com.inseedent.exception.ResourceNotFoundException;
import com.inseedent.repository.DeploymentRepository;
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
@RequestMapping("/api/v1/incidents/{incidentId}/deployments")
public class DeploymentController {

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private TimelineService timelineService;

    /**
     * List all deployments for an incident
     * GET /api/v1/incidents/{incidentId}/deployments
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Deployment>>> listDeployments(@PathVariable Long incidentId) {
        log.info("GET /api/v1/incidents/{}/deployments - Listing deployments", incidentId);

        try {
            if (!incidentRepository.existsById(incidentId)) {
                throw new ResourceNotFoundException("Incident not found with id: " + incidentId);
            }

            List<Deployment> deployments = deploymentRepository.findByIncidentId(incidentId);
            return ResponseEntity.ok(ApiResponse.success(deployments));
        } catch (ResourceNotFoundException e) {
            log.warn("Incident not found: {}", incidentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error listing deployments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error listing deployments: " + e.getMessage()));
        }
    }

    /**
     * Ingest a new deployment for an incident
     * POST /api/v1/incidents/{incidentId}/deployments
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Deployment>> createDeployment(
            @PathVariable Long incidentId,
            @Valid @RequestBody Deployment deployment) {
        
        log.info("POST /api/v1/incidents/{}/deployments - Creating deployment: {}", incidentId, deployment.getServiceName());

        try {
            if (!incidentRepository.existsById(incidentId)) {
                throw new ResourceNotFoundException("Incident not found with id: " + incidentId);
            }

            // Validate deployment
            if (deployment.getServiceName() == null || deployment.getServiceName().trim().isEmpty()) {
                throw new IllegalArgumentException("Service name cannot be empty");
            }

            // Set incident and deployment time if not provided
            deployment.setIncident(incidentRepository.findById(incidentId).get());
            if (deployment.getDeploymentTime() == null) {
                deployment.setDeploymentTime(LocalDateTime.now());
            }

            Deployment savedDeployment = deploymentRepository.save(deployment);

            // Auto-add timeline event for each deployment
            String description = String.format("Deployment: %s version %s (previous: %s) by %s", 
                    savedDeployment.getServiceName(),
                    savedDeployment.getVersion() != null ? savedDeployment.getVersion() : "unknown",
                    savedDeployment.getPreviousVersion() != null ? savedDeployment.getPreviousVersion() : "unknown",
                    savedDeployment.getDeployedBy() != null ? savedDeployment.getDeployedBy() : "unknown");
            
            String deploymentData = String.format("{\"service\": \"%s\", \"version\": \"%s\", \"previousVersion\": \"%s\", \"rollbackAvailable\": %s}", 
                    savedDeployment.getServiceName(),
                    savedDeployment.getVersion(),
                    savedDeployment.getPreviousVersion(),
                    savedDeployment.getRollbackAvailable());
            
            timelineService.addTimelineEvent(incidentId, "deployment", description, 
                    "medium", savedDeployment.getServiceName(), deploymentData);

            log.info("Deployment created successfully for incident {}: id={}", incidentId, savedDeployment.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(savedDeployment));
        } catch (ResourceNotFoundException e) {
            log.warn("Incident not found: {}", incidentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid deployment input: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating deployment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error creating deployment: " + e.getMessage()));
        }
    }
}
