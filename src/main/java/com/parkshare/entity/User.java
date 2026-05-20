package com.parkshare.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Entidad principal de usuario del sistema ParkShare.
 * Implementa UserDetails para integración con Spring Security.
 * Los roles determinan si el usuario puede publicar cocheras (HOST)
 * o reservar estacionamientos (DRIVER).
 */
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = false)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email único que actúa como nombre de usuario en el sistema */
    @Column(unique = true, nullable = false)
    private String email;

    /** Contraseña almacenada con hash BCrypt — NUNCA en texto plano */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private String phone;

    /** Rol del usuario: DRIVER para conductores, HOST para propietarios de cocheras */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Cuenta habilitada por defecto al momento del registro */
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * Token de Firebase Cloud Messaging para notificaciones push.
     * Se actualiza en cada login desde un nuevo dispositivo.
     * Puede ser nulo si el usuario nunca ha hecho login desde un dispositivo móvil.
     */
    @Column(name = "fcm_token")
    private String fcmToken;

    // ==================== UserDetails ====================
    // Métodos requeridos por Spring Security para autenticación y autorización

    /**
     * Retorna la autoridad del usuario basada en su rol.
     * Se usa el prefijo ROLE_ que Spring Security espera por convención.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /** El email es el identificador único usado para autenticación */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // ==================== Enum de Roles ====================

    public enum Role {
        DRIVER,
        HOST
    }
}
