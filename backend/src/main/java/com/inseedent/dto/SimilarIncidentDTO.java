package com.inseedent.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarIncidentDTO {
    private Long incidentId;
    private String title;
    private Double similarityScore;
    private String rootCause;
}
