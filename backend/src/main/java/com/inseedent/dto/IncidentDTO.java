package com.inseedent.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentDTO {
    private Long id;
    private String title;
    private String description;
    private String severity;
    private String status;
    private String[] affectedServices;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String detectedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
