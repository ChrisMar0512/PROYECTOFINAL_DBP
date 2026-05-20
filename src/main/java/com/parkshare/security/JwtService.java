package com.parkshare.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Servicio para generación y validación de tokens JWT.
 *
 * SEGURIDAD:
 * - Usa HMAC-SHA256 (HS256) como algoritmo de firma.
 * - El secreto se inyecta desde application.properties y debe tener
 *   al menos 64 caracteres para cumplir el mínimo de HS256.
 * - NUNCA incluir información sensible en el payload del token.
 */
@Service
public class JwtService {

    /** Secreto para firmar los tokens — debe ser de al menos 64 caracteres */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /** Tiempo de expiración del token en milisegundos */
    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Genera un token JWT para el usuario autenticado.
     *
     * @param userDetails detalles del usuario autenticado
     * @return token JWT firmado con HS256
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Genera un token JWT con claims adicionales.
     *
     * @param extraClaims claims adicionales a incluir en el payload
     * @param userDetails detalles del usuario autenticado
     * @return token JWT firmado
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())          // email del usuario
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // firma HS256
                .compact();
    }

    /**
     * Extrae el email (subject) de un token JWT.
     *
     * @param token el token JWT
     * @return email del usuario contenido en el token
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Verifica si un token JWT es válido para el usuario dado.
     * Valida que el email coincida y que el token no haya expirado.
     *
     * @param token       el token JWT a validar
     * @param userDetails detalles del usuario a verificar
     * @return true si el token es válido, false en caso contrario
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        // SEGURIDAD: verificar que el token pertenece al usuario y no está expirado
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    // ==================== Métodos privados de utilidad ====================

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        // SEGURIDAD: la verificación de la firma ocurre aquí;
        // si la firma es inválida se lanza una excepción automáticamente
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Construye la clave de firma HMAC a partir del secreto configurado.
     * El secreto se decodifica desde Base64 para mayor seguridad.
     */
    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
