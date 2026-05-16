package com.inseedent.service.incident;

import com.inseedent.domain.Incident;
import com.inseedent.dto.IncidentDTO;
import com.inseedent.exception.ResourceNotFoundException;
import com.inseedent.repository.IncidentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class IncidentService {

    @Autowired
    private IncidentRepository incidentRepository;

    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * Create a new incident
     */
    @Transactional
    public IncidentDTO createIncident(IncidentDTO incidentDTO) {
        log.info("Creating new incident: {}", incidentDTO.getTitle());

        validateIncidentDTO(incidentDTO);

        Incident incident = Incident.builder()
                .title(incidentDTO.getTitle())
                .description(incidentDTO.getDescription())
                .severity(incidentDTO.getSeverity())
                .status(incidentDTO.getStatus() != null ? incidentDTO.getStatus() : "open")
                .affectedServices(incidentDTO.getAffectedServices())
                .startTime(incidentDTO.getStartTime() != null ? incidentDTO.getStartTime() : LocalDateTime.now())
                .endTime(incidentDTO.getEndTime())
                .detectedBy(incidentDTO.getDetectedBy())
                .build();

        Incident savedIncident = incidentRepository.save(incident);
        log.info("Incident created successfully with id: {}", savedIncident.getId());

        return mapToDTO(savedIncident);
    }

    /**
     * Get incident by ID
     */
    public IncidentDTO getIncident(Long id) {
        log.debug("Fetching incident with id: {}", id);

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Incident not found with id: {}", id);
                    return new ResourceNotFoundException("Incident not found with id: " + id);
                });

        return mapToDTO(incident);
    }

    /**
     * List incidents with pagination and filtering
     * Default page size: 10, sorted by startTime DESC
     */
    public Page<IncidentDTO> listIncidents(int page, int size, String status, String severity) {
        log.debug("Listing incidents: page={}, size={}, status={}, severity={}", page, size, status, severity);

        if (size <= 0 || size > 100) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (page < 0) {
            page = 0;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());

        Page<Incident> incidents;

        // Apply primary filter based on what's provided
        if (status != null && !status.isEmpty()) {
            incidents = incidentRepository.findByStatus(status, pageable);
        } else if (severity != null && !severity.isEmpty()) {
            incidents = incidentRepository.findBySeverity(severity, pageable);
        } else {
            incidents = incidentRepository.findAll(pageable);
        }

        return incidents.map(this::mapToDTO);
    }

    /**
     * Update an incident
     */
    @Transactional
    public IncidentDTO updateIncident(Long id, IncidentDTO incidentDTO) {
        log.info("Updating incident with id: {}", id);

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Incident not found with id: {}", id);
                    return new ResourceNotFoundException("Incident not found with id: " + id);
                });

        if (incidentDTO.getTitle() != null && !incidentDTO.getTitle().isEmpty()) {
            incident.setTitle(incidentDTO.getTitle());
        }
        if (incidentDTO.getDescription() != null) {
            incident.setDescription(incidentDTO.getDescription());
        }
        if (incidentDTO.getSeverity() != null && !incidentDTO.getSeverity().isEmpty()) {
            incident.setSeverity(incidentDTO.getSeverity());
        }
        if (incidentDTO.getStatus() != null && !incidentDTO.getStatus().isEmpty()) {
            incident.setStatus(incidentDTO.getStatus());
        }
        if (incidentDTO.getAffectedServices() != null) {
            incident.setAffectedServices(incidentDTO.getAffectedServices());
        }
        if (incidentDTO.getStartTime() != null) {
            incident.setStartTime(incidentDTO.getStartTime());
        }
        if (incidentDTO.getEndTime() != null) {
            incident.setEndTime(incidentDTO.getEndTime());
        }
        if (incidentDTO.getDetectedBy() != null) {
            incident.setDetectedBy(incidentDTO.getDetectedBy());
        }

        Incident updatedIncident = incidentRepository.save(incident);
        log.info("Incident updated successfully with id: {}", id);

        return mapToDTO(updatedIncident);
    }

    /**
     * Delete an incident
     */
    @Transactional
    public void deleteIncident(Long id) {
        log.info("Deleting incident with id: {}", id);

        if (!incidentRepository.existsById(id)) {
            log.warn("Incident not found with id: {}", id);
            throw new ResourceNotFoundException("Incident not found with id: " + id);
        }

        incidentRepository.deleteById(id);
        log.info("Incident deleted successfully with id: {}", id);
    }

    /**
     * Map Incident entity to IncidentDTO
     */
    private IncidentDTO mapToDTO(Incident incident) {
        return IncidentDTO.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .severity(incident.getSeverity())
                .status(incident.getStatus())
                .affectedServices(incident.getAffectedServices())
                .startTime(incident.getStartTime())
                .endTime(incident.getEndTime())
                .detectedBy(incident.getDetectedBy())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }

    /**
     * Validate IncidentDTO
     */
    private void validateIncidentDTO(IncidentDTO incidentDTO) {
        if (incidentDTO.getTitle() == null || incidentDTO.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Incident title cannot be empty");
        }
        if (incidentDTO.getSeverity() == null || incidentDTO.getSeverity().trim().isEmpty()) {
            throw new IllegalArgumentException("Incident severity cannot be empty");
        }
    }
}
