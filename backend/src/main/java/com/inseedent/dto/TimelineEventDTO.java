package com.inseedent.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEventDTO {
    private Long id;
    private String eventType;
    private String eventSource;
    private String description;
    private String severity;
    private LocalDateTime eventTime;
    private String eventData;
}
