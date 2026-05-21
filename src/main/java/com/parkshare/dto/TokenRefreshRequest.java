package com.parkshare.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO para renovar el JWT usando un Refresh Token.
 */
@Data
public class TokenRefreshRequest {

    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;
}
