package com.parkshare.controller;

import com.parkshare.dto.AuthResponse;
import com.parkshare.dto.LoginRequest;
import com.parkshare.dto.RegisterRequest;
import com.parkshare.entity.User;
import com.parkshare.repository.UserRepository;
import com.parkshare.security.JwtService;
import com.parkshare.service.WalletService;
import com.parkshare.exception.DuplicateResourceException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * Proceso:
     * 1. Valida los datos del DTO con @Valid
     * 2. Verifica que el email no esté en uso
     * 3. Encripta la contraseña con BCrypt
     * 4. Guarda el usuario en BD
     * 5. Inicializa la billetera del usuario
     * 6. Genera y retorna el JWT
     *
     * @param request datos del nuevo usuario
     * @return AuthResponse con JWT y datos básicos del usuario
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // Verificar que el email no esté registrado
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Ya existe una cuenta con el email: " + request.getEmail());
        }

        // Construir entidad User
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        // SEGURIDAD: encriptar contraseña con BCrypt antes de persistir
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        // Inicializar billetera con saldo cero (Integrante 2)
        walletService.initializeWallet(savedUser);

        String jwt = jwtService.generateToken(
                java.util.Map.of("role", savedUser.getRole().name()), savedUser
        );

        // Publicar evento para envío asíncrono de email de bienvenida
        eventPublisher.publishEvent(new com.parkshare.event.UserRegisteredEvent(
                this, savedUser.getId(), savedUser.getEmail(), savedUser.getName()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(jwt, savedUser));
    }

    /**
     * Autentica un usuario existente y retorna un JWT.
     *
     * Proceso:
     * 1. Delega la autenticación al AuthenticationManager (verifica email + BCrypt)
     * 2. Si el request incluye fcmToken, actualiza el dispositivo del usuario
     * 3. Genera y retorna el JWT
     *
     * @param request credenciales del usuario (email, password, fcmToken opcional)
     * @return AuthResponse con JWT y datos básicos del usuario
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // SEGURIDAD: AuthenticationManager verifica credenciales con BCrypt
        // Lanza BadCredentialsException automáticamente si son incorrectas
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User) authentication.getPrincipal();

        // Actualizar fcmToken si el cliente lo proveyó (login desde nuevo dispositivo)
        if (request.getFcmToken() != null && !request.getFcmToken().isBlank()) {
            user.setFcmToken(request.getFcmToken());
            userRepository.save(user);
        }

        String jwt = jwtService.generateToken(
                java.util.Map.of("role", user.getRole().name()), user
        );
        return ResponseEntity.ok(new AuthResponse(jwt, user));
    }
}
