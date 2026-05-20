package com.parkshare.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Representa un espacio de estacionamiento publicado por un HOST en ParkShare.
 *
 * COORDENADAS — SRID 4326 (WGS84):
 *   El estándar WGS84 es el sistema de referencia geodésico utilizado por GPS.
 *   Longitud en eje X, Latitud en eje Y. PostGIS usa este SRID para operaciones
 *   espaciales como ST_DWithin (búsqueda por radio en metros cuando se usa
 *   el tipo ::geography que convierte el cálculo a metros reales en la superficie).
 *
 * FOTO:
 *   photoUrl      — URL pública HTTPS devuelta por Cloudinary al subir la imagen.
 *   cloudinaryPublicId — identificador de la imagen en Cloudinary; necesario para
 *                        poder eliminarla cuando el host actualice o borre la cochera.
 */
@Entity
@Table(name = "parking_space")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ParkingSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Propietario de la cochera — debe tener rol HOST */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String address;

    /** Precio por hora en soles peruanos (PEN), con hasta 2 decimales */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    /** Estado actual del espacio de estacionamiento */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParkingSpaceStatus status = ParkingSpaceStatus.AVAILABLE;

    /**
     * Coordenadas geográficas del espacio de estacionamiento.
     * SRID 4326 = WGS84, el sistema estándar de GPS (longitud X, latitud Y).
     * PostGIS utiliza este SRID para operaciones espaciales precisas en metros
     * al convertir a tipo geography (ST_DWithin con ::geography).
     */
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;

    /**
     * URL segura (HTTPS) de la foto principal de la cochera, almacenada en Cloudinary.
     * Puede ser nula si el host no subió foto al crear el espacio.
     */
    @Column(name = "photo_url")
    private String photoUrl;

    /**
     * Identificador público en Cloudinary de la foto de la cochera.
     * Se persiste para poder llamar a cloudinary.uploader().destroy(publicId)
     * cuando el host actualice o elimine la foto, evitando imágenes huérfanas.
     */
    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;

    /** Características/amenidades de la cochera (Techado, Seguridad 24h, etc.) */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "parking_space_features",
            joinColumns = @JoinColumn(name = "parking_space_id"),
            inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    private Set<Feature> features = new HashSet<>();

    /**
     * Usuarios (DRIVER) que han marcado esta cochera como favorita.
     * La tabla intermedia user_favorite_spaces permite consultas bidireccionales:
     * - Qué cocheras son favoritas de un usuario
     * - Cuántos favoritos tiene una cochera
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_favorite_spaces",
            joinColumns = @JoinColumn(name = "parking_space_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> favoritedBy = new HashSet<>();

    /** Fecha y hora de creación, gestionada automáticamente por Hibernate */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ==================== Enum de Estados ====================

    public enum ParkingSpaceStatus {
        /** La cochera está disponible para reservas */
        AVAILABLE,
        /** La cochera tiene una reserva activa confirmada */
        RESERVED,
        /** La cochera está físicamente ocupada */
        OCCUPIED
    }
}
