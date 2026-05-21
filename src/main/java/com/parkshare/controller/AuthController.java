package com.parkshare.controller;

import com.parkshare.dto.AuthResponse;
import com.parkshare.dto.LoginRequest;
import com.parkshare.dto.RegisterRequest;
import com.parkshare.dto.TokenRefreshRequest;
import com.parkshare.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación de ParkShare.
 * Maneja el registro de nuevos usuarios y el inicio de sesión.
 *
 * Rutas públicas (sin JWT):
 *   POST /api/auth/register — crear cuenta nueva
 *   POST /api/auth/login    — iniciar sesión y obtener JWT
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param request datos del nuevo usuario
     * @return AuthResponse con JWT y datos básicos del usuario
     */
    @PostMapping("/register-user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Autentica un usuario existente y retorna un JWT.
     *
     * @param request credenciales del usuario (email, password, fcmToken opcional)
     * @return AuthResponse con JWT y datos básicos del usuario
     */
    @PostMapping("/login-user")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Renueva un JWT expirado usando un Refresh Token.
     *
     * @param request DTO con el refresh token
     * @return AuthResponse con un nuevo JWT y el mismo refresh token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        AuthResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
}
