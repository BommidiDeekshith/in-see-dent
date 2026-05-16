package com.inseedent.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private Long incidentId;
    private String message;
    private String sessionId;
}
