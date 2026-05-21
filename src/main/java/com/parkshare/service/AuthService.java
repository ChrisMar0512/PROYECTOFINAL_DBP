package com.parkshare.service;

import com.parkshare.dto.AuthResponse;
import com.parkshare.dto.LoginRequest;
import com.parkshare.dto.RegisterRequest;
import com.parkshare.entity.RefreshToken;
import com.parkshare.entity.User;
import com.parkshare.exception.DuplicateResourceException;
import com.parkshare.repository.RefreshTokenRepository;
import com.parkshare.repository.UserRepository;
import com.parkshare.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para la gestión de autenticación y registro de usuarios en ParkShare.
 * Encapsula la lógica de negocio que estaba previamente en AuthController.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param request datos del nuevo usuario
     * @return AuthResponse con JWT y datos del usuario
     */
    public AuthResponse register(RegisterRequest request) {
        // Verificar que el email no esté registrado
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Ya existe una cuenta con el email: " + request.getEmail());
        }

        // Construir entidad User
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        // Inicializar billetera con saldo cero
        walletService.initializeWallet(savedUser);

        // Generar JWT con claims de rol
        String jwt = jwtService.generateToken(
                Map.of("role", savedUser.getRole().name()), savedUser
        );

        // Publicar evento para envío asíncrono de email de bienvenida
        eventPublisher.publishEvent(new com.parkshare.event.UserRegisteredEvent(
                this, savedUser.getId(), savedUser.getEmail(), savedUser.getName()
        ));

        // Generar refresh token
        RefreshToken refreshToken = createRefreshToken(savedUser.getId());

        log.info("Usuario registrado exitosamente: id={}, email={}", savedUser.getId(), savedUser.getEmail());
        return new AuthResponse(jwt, refreshToken.getToken(), savedUser);
    }

    /**
     * Autentica un usuario existente y retorna un JWT.
     *
     * @param request credenciales del usuario
     * @return AuthResponse con JWT y datos del usuario
     */
    public AuthResponse login(LoginRequest request) {
        // Delegar la autenticación al AuthenticationManager (verifica email + BCrypt)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User) authentication.getPrincipal();

        // Actualizar fcmToken si el cliente lo proveyó
        if (request.getFcmToken() != null && !request.getFcmToken().isBlank()) {
            user.setFcmToken(request.getFcmToken());
            userRepository.save(user);
            log.info("FCM Token actualizado para usuario id={}", user.getId());
        }

        // Generar JWT con claims de rol
        String jwt = jwtService.generateToken(
                Map.of("role", user.getRole().name()), user
        );

        // Generar refresh token
        RefreshToken refreshToken = createRefreshToken(user.getId());

        log.info("Usuario autenticado exitosamente: id={}, email={}", user.getId(), user.getEmail());
        return new AuthResponse(jwt, refreshToken.getToken(), user);
    }

    /**
     * Crea o actualiza el Refresh Token para un usuario.
     * Expiración establecida en 7 días (604,800,000 ms).
     */
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        // Eliminar tokens previos del usuario para mantener una sesión única
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(604800000));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Valida un Refresh Token y emite un nuevo JWT de acceso.
     *
     * @param requestRefreshToken token de refresco provisto por el cliente
     * @return AuthResponse con el nuevo JWT y el mismo refresh token
     */
    public AuthResponse refreshAccessToken(String requestRefreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new com.parkshare.exception.UnauthorizedOperationException(
                        "Refresh token no registrado en el sistema."));

        // Verificar expiración
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new com.parkshare.exception.UnauthorizedOperationException(
                    "El refresh token ha expirado. Por favor inicie sesión nuevamente.");
        }

        User user = token.getUser();
        String jwt = jwtService.generateToken(
                Map.of("role", user.getRole().name()), user
        );

        log.info("JWT renovado exitosamente para usuario: {}", user.getEmail());
        return new AuthResponse(jwt, token.getToken(), user);
    }
}
