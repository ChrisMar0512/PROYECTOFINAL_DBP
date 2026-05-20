package com.parkshare.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Reseña de un usuario hacia otro después de completar una reserva.
 *
 * Un DRIVER puede reseñar al HOST y viceversa tras una reserva FINISHED.
 * La combinación (reservation_id, reviewer_id) es única a nivel de BD para
 * garantizar que un mismo usuario no pueda dejar dos reseñas en la misma reserva.
 *
 * reviewer = quien escribe la reseña
 * reviewee = quien recibe la reseña (rol opuesto al reviewer en la reserva)
 */
@Entity
@Table(
    name = "review",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_review_reservation_reviewer",
            columnNames = {"reservation_id", "reviewer_id"}
        )
    }
)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Reserva que originó esta reseña — garantiza contexto válido */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    /** Usuario que escribió la reseña (DRIVER o HOST) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    /** Usuario que recibe la reseña (rol opuesto al reviewer en la reserva) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee;

    /** Cochera donde se realizó la reserva */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_space_id", nullable = false)
    private ParkingSpace parkingSpace;

    /** Calificación del 1 al 5 */
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer rating;

    /** Comentario opcional del reviewer */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /** Momento de creación de la reseña */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
