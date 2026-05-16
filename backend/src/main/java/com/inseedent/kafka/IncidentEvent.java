package com.inseedent.kafka;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentEvent {
    private String eventType;
    private Long incidentId;
    private String title;
    private String severity;
}
