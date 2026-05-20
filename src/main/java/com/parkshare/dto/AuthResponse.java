package com.parkshare.dto;

import com.parkshare.entity.User;
import lombok.Data;

/**
 * DTO de respuesta para operaciones de autenticación (registro y login).
 * Contiene el JWT y los datos básicos del usuario para que el cliente
 * pueda identificar al usuario sin hacer llamadas adicionales.
 */
@Data
public class AuthResponse {

    /** JWT firmado — el cliente debe enviarlo en el header Authorization: Bearer <token> */
    private String token;

    private Long userId;
    private String name;
    private String email;
    private String role;

    public AuthResponse(String token, User user) {
        this.token = token;
        this.userId = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole().name();
    }
}
