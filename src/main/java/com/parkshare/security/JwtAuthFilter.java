package com.parkshare.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT que intercepta cada petición HTTP exactamente una vez.
 *
 * SEGURIDAD:
 * - Extrae el token del header Authorization (formato: Bearer <token>).
 * - Valida el token con JwtService antes de setear el contexto de seguridad.
 * - Si el token es inválido o ausente, la petición continúa sin autenticación
 *   y Spring Security aplicará las reglas de acceso configuradas en SecurityConfig.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Extraer header de autorización
        final String authHeader = request.getHeader("Authorization");

        // SEGURIDAD: verificar que el header existe y tiene el formato correcto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer el token eliminando el prefijo "Bearer "
        final String jwt = authHeader.substring(7);
        final String userEmail;

        try {
            userEmail = jwtService.extractEmail(jwt);
        } catch (Exception e) {
            // SEGURIDAD: token malformado o con firma inválida — rechazar silenciosamente
            filterChain.doFilter(request, response);
            return;
        }

        // Solo autenticar si se extrajo un email y aún no hay autenticación en el contexto
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            // SEGURIDAD: validar que el token corresponde al usuario y no está expirado
            if (jwtService.isTokenValid(jwt, userDetails)) {

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,                          // no se necesitan credenciales tras validación JWT
                        userDetails.getAuthorities()
                );

                // Agregar detalles de la petición HTTP al token de autenticación
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Setear autenticación en el contexto de seguridad de Spring
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
