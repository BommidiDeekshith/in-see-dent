package com.inseedent.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResultDTO {
    private Long id;
    private Long incidentId;
    private String status;
    private String rootCause;
    private Double confidenceScore;
    private String[] affectedServices;
    private String summary;
    private Object findings;
    private String remediationSuggestions;
}
