package com.book_store.user_service.dto;

import lombok.Builder;
import lombok.Data;
@Builder
@Data
public class TokenResponse {
    private String token;
//    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private String userId;
    private String role;
    private String name;
    private String email;
}
