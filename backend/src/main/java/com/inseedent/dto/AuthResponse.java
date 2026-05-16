package com.inseedent.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private Long userId;
    private String username;
    private String email;
    private String token;
    private String refreshToken;
    private long expiresIn;
}
