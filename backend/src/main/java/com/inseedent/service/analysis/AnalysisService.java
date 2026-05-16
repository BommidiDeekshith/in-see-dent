package com.inseedent.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inseedent.client.AiOrchestrationClient;
import com.inseedent.domain.*;
import com.inseedent.dto.AnalysisResultDTO;
import com.inseedent.exception.ResourceNotFoundException;
import com.inseedent.repository.*;
import com.inseedent.service.incident.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AiOrchestrationClient aiClient;
    private final IncidentRepository incidentRepository;
    private final AiAnalysisRepository analysisRepository;
    private final AnalysisArtifactRepository artifactRepository;
    private final AlertRepository alertRepository;
    private final LogRepository logRepository;
    private final MetricRepository metricRepository;
    private final TraceRepository traceRepository;
    private final DeploymentRepository deploymentRepository;
    private final TimelineService timelineService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AnalysisResultDTO triggerAnalysis(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentId));

        AiAnalysis analysis = AiAnalysis.builder()
                .incident(incident)
                .analysisStatus("running")
                .startedAt(LocalDateTime.now())
                .build();
        analysis = analysisRepository.save(analysis);

        timelineService.addTimelineEvent(incidentId, "ai_analysis", "AI root cause analysis started",
                "info", "inseedent-ai", null);

        runAnalysisAsync(analysis.getId(), incidentId);
        return toDto(analysis);
    }

    @Async
    public void runAnalysisAsync(Long analysisId, Long incidentId) {
        try {
            Map<String, Object> payload = buildAnalysisPayload(incidentId);
            JsonNode result = aiClient.analyzeIncident(payload);

            AiAnalysis analysis = analysisRepository.findById(analysisId).orElseThrow();
            analysis.setAnalysisStatus("completed");
            analysis.setRootCause(result.path("root_cause").asText());
            analysis.setConfidenceScore(result.path("confidence_score").asDouble(0.0));
            analysis.setSummary(result.path("summary").asText());
            analysis.setRemediationSuggestions(result.path("remediation_suggestions").asText());
            analysis.setFindings(result.path("findings").toString());
            analysis.setAnalysisData(result.toString());
            analysis.setCompletedAt(LocalDateTime.now());

            if (result.has("affected_services") && result.get("affected_services").isArray()) {
                List<String> services = new ArrayList<>();
                result.get("affected_services").forEach(n -> services.add(n.asText()));
                analysis.setAffectedServices(services.toArray(new String[0]));
            }

            analysisRepository.save(analysis);
            saveArtifacts(analysis, result);
            timelineService.addTimelineEvent(incidentId, "ai_analysis",
                    "AI analysis completed — confidence " + analysis.getConfidenceScore(),
                    "info", "inseedent-ai", null);
            log.info("Analysis {} completed for incident {}", analysisId, incidentId);
        } catch (Exception e) {
            log.error("Analysis failed for incident {}", incidentId, e);
            analysisRepository.findById(analysisId).ifPresent(a -> {
                a.setAnalysisStatus("failed");
                a.setSummary("Analysis failed: " + e.getMessage());
                a.setCompletedAt(LocalDateTime.now());
                analysisRepository.save(a);
            });
        }
    }

    public AnalysisResultDTO getLatestAnalysis(Long incidentId) {
        return analysisRepository.findByIncident_IdOrderByCreatedAtDesc(incidentId)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("No analysis found for incident: " + incidentId));
    }

    public List<AnalysisResultDTO> listAnalyses(Long incidentId) {
        return analysisRepository.findByIncident_Id(incidentId).stream().map(this::toDto).toList();
    }

    private void saveArtifacts(AiAnalysis analysis, JsonNode result) {
        if (!result.has("artifacts")) return;
        result.get("artifacts").forEach(node -> {
            AnalysisArtifact artifact = AnalysisArtifact.builder()
                    .analysis(analysis)
                    .artifactType(node.path("type").asText())
                    .agentName(node.path("agent").asText())
                    .findings(node.path("findings").asText())
                    .confidence(node.path("confidence").asDouble(0))
                    .rawData(node.toString())
                    .build();
            artifactRepository.save(artifact);
        });
    }

    private Map<String, Object> buildAnalysisPayload(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId).orElseThrow();
        Map<String, Object> payload = new HashMap<>();
        payload.put("incident_id", incidentId);
        payload.put("title", incident.getTitle());
        payload.put("description", incident.getDescription());
        payload.put("severity", incident.getSeverity());
        payload.put("affected_services", incident.getAffectedServices());
        payload.put("alerts", alertRepository.findByIncident_Id(incidentId));
        payload.put("logs", logRepository.findByIncident_Id(incidentId).stream().limit(50).toList());
        payload.put("metrics", metricRepository.findByIncident_Id(incidentId).stream().limit(50).toList());
        payload.put("traces", traceRepository.findByIncident_Id(incidentId).stream().limit(30).toList());
        payload.put("deployments", deploymentRepository.findByIncident_Id(incidentId));
        return payload;
    }

    private AnalysisResultDTO toDto(AiAnalysis a) {
        return AnalysisResultDTO.builder()
                .id(a.getId())
                .incidentId(a.getIncident().getId())
                .status(a.getAnalysisStatus())
                .rootCause(a.getRootCause())
                .confidenceScore(a.getConfidenceScore())
                .affectedServices(a.getAffectedServices())
                .summary(a.getSummary())
                .remediationSuggestions(a.getRemediationSuggestions())
                .build();
    }
}
