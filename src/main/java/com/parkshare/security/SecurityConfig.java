package com.parkshare.security;

import com.parkshare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración central de seguridad de ParkShare.
 *
 * SEGURIDAD:
 * - CSRF deshabilitado: la API es stateless (JWT), no usa cookies de sesión.
 * - Sesión STATELESS: no se crea ni mantiene HttpSession en el servidor.
 * - Solo las rutas de autenticación (/api/auth/**) y WebSocket (/ws/**) son públicas.
 * - Todo lo demás requiere un JWT válido en el header Authorization.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserRepository userRepository;

    /**
     * Cadena de filtros de seguridad principal.
     * Define qué rutas son públicas y cuáles requieren autenticación.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // SEGURIDAD: deshabilitar CSRF porque usamos JWT (sin estado de sesión)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS: permitir peticiones del frontend
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOrigins(java.util.List.of("http://localhost:3000", "http://localhost:5173"));
                    config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(java.util.List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))

                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas: registro y login
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // Rutas públicas: endpoint WebSocket
                        .requestMatchers("/ws/**").permitAll()
                        // Rutas públicas: Swagger UI / OpenAPI
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )

                // SEGURIDAD: política sin estado — no crear ni usar HttpSession
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authenticationProvider(authenticationProvider())

                // Insertar el filtro JWT antes del filtro estándar de usuario/contraseña
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Servicio que carga los usuarios desde la base de datos por email.
     * Usado por Spring Security en el proceso de autenticación.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con email: " + email
                ));
    }

    /**
     * Proveedor de autenticación DAO que usa BCrypt para verificar contraseñas.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        // SEGURIDAD: usar BCrypt para comparar contraseñas hasheadas
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Expone el AuthenticationManager como bean para uso en AuthController.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Encoder BCrypt con factor de costo por defecto (10 rounds).
     * SEGURIDAD: BCrypt incluye salt automáticamente — nunca usar MD5 o SHA-1.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
