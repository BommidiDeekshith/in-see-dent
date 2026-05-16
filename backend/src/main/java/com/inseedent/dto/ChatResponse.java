package com.inseedent.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String reply;
    private String sessionId;
    private List<String> suggestedPrompts;
    private List<SimilarIncidentDTO> similarIncidents;
}
