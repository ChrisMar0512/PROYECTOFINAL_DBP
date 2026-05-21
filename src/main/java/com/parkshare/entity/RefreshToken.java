package com.parkshare.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Entidad que representa un token de refresco (Refresh Token) en el sistema.
 * Se usa para solicitar nuevos JWT sin que el usuario tenga que re-autenticarse con credenciales.
 */
@Entity
@Table(name = "refresh_token")
@Data
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID aleatorio que actúa como Refresh Token */
    @Column(nullable = false, unique = true)
    private String token;

    /** Usuario asociado a este token */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    /** Fecha y hora de expiración */
    @Column(nullable = false)
    private Instant expiryDate;
}
