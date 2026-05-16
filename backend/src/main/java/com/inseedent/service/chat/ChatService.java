package com.inseedent.service.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.inseedent.client.AiOrchestrationClient;
import com.inseedent.domain.Incident;
import com.inseedent.dto.ChatRequest;
import com.inseedent.dto.ChatResponse;
import com.inseedent.dto.SimilarIncidentDTO;
import com.inseedent.exception.ResourceNotFoundException;
import com.inseedent.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final AiOrchestrationClient aiClient;
    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;
    private final DeploymentRepository deploymentRepository;
    private final AiAnalysisRepository analysisRepository;

    public ChatResponse chat(ChatRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", request.getMessage());
        payload.put("session_id", request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString());

        if (request.getIncidentId() != null) {
            Incident incident = incidentRepository.findById(request.getIncidentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
            payload.put("incident_id", incident.getId());
            payload.put("incident_title", incident.getTitle());
            payload.put("affected_services", incident.getAffectedServices());
            payload.put("alerts", alertRepository.findByIncident_Id(incident.getId()));
            payload.put("deployments", deploymentRepository.findByIncident_Id(incident.getId()));
            analysisRepository.findByIncident_IdOrderByCreatedAtDesc(incident.getId())
                    .ifPresent(a -> payload.put("latest_analysis", a.getSummary()));
        }

        JsonNode result = aiClient.chat(payload);
        List<SimilarIncidentDTO> similar = new ArrayList<>();
        if (result.has("similar_incidents")) {
            result.get("similar_incidents").forEach(n -> similar.add(SimilarIncidentDTO.builder()
                    .incidentId(n.path("incident_id").asLong())
                    .title(n.path("title").asText())
                    .similarityScore(n.path("score").asDouble())
                    .rootCause(n.path("root_cause").asText(null))
                    .build()));
        }

        return ChatResponse.builder()
                .reply(result.path("reply").asText())
                .sessionId(result.path("session_id").asText(payload.get("session_id").toString()))
                .suggestedPrompts(List.of(
                        "Why did checkout-service fail?",
                        "Show similar incidents",
                        "What changed before the outage?",
                        "What remediation steps do you recommend?"
                ))
                .similarIncidents(similar)
                .build();
    }
}
