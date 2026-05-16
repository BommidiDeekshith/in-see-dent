package com.inseedent.controller;

import com.inseedent.dto.IncidentDTO;
import com.inseedent.exception.ResourceNotFoundException;
import com.inseedent.service.incident.IncidentService;
import com.inseedent.util.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    /**
     * List incidents with pagination and filtering
     * GET /api/v1/incidents?page=0&size=10&status=open&severity=critical
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<IncidentDTO>>> listIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity) {
        
        log.info("GET /api/v1/incidents - page={}, size={}, status={}, severity={}", page, size, status, severity);

        try {
            Page<IncidentDTO> incidents = incidentService.listIncidents(page, size, status, severity);
            return ResponseEntity.ok(ApiResponse.success(incidents));
        } catch (Exception e) {
            log.error("Error listing incidents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error listing incidents: " + e.getMessage()));
        }
    }

    /**
     * Get a single incident by ID with timeline events
     * GET /api/v1/incidents/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<IncidentDTO>> getIncident(@PathVariable Long id) {
        log.info("GET /api/v1/incidents/{} - Fetching incident details", id);

        try {
            IncidentDTO incident = incidentService.getIncident(id);
            return ResponseEntity.ok(ApiResponse.success(incident));
        } catch (ResourceNotFoundException e) {
            log.warn("Incident not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching incident", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error fetching incident: " + e.getMessage()));
        }
    }

    /**
     * Create a new incident
     * POST /api/v1/incidents
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<IncidentDTO>> createIncident(@Valid @RequestBody IncidentDTO incidentDTO) {
        log.info("POST /api/v1/incidents - Creating new incident: {}", incidentDTO.getTitle());

        try {
            IncidentDTO createdIncident = incidentService.createIncident(incidentDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(createdIncident));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for incident creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating incident", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error creating incident: " + e.getMessage()));
        }
    }

    /**
     * Update an incident
     * PUT /api/v1/incidents/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<IncidentDTO>> updateIncident(
            @PathVariable Long id,
            @Valid @RequestBody IncidentDTO incidentDTO) {
        
        log.info("PUT /api/v1/incidents/{} - Updating incident", id);

        try {
            IncidentDTO updatedIncident = incidentService.updateIncident(id, incidentDTO);
            return ResponseEntity.ok(ApiResponse.success(updatedIncident));
        } catch (ResourceNotFoundException e) {
            log.warn("Incident not found for update: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for incident update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating incident", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error updating incident: " + e.getMessage()));
        }
    }

    /**
     * Delete an incident
     * DELETE /api/v1/incidents/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteIncident(@PathVariable Long id) {
        log.info("DELETE /api/v1/incidents/{} - Deleting incident", id);

        try {
            incidentService.deleteIncident(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (ResourceNotFoundException e) {
            log.warn("Incident not found for deletion: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting incident", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error deleting incident: " + e.getMessage()));
        }
    }
}
