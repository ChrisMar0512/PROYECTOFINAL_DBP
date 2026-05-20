package com.parkshare.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa una reserva de estacionamiento en ParkShare.
 *
 * FLUJO DE VIDA DE UNA RESERVA:
 *   1. PENDING  — creada por el DRIVER, tiene 15 minutos (expiresAt) para hacer check-in.
 *   2. ACTIVE   — el DRIVER hizo check-in (startTime se asigna).
 *   3. FINISHED — el DRIVER hizo check-out (endTime se asigna).
 *   4. EXPIRED  — el scheduler detectó que expiresAt pasó sin check-in, o el DRIVER canceló.
 *
 * ÍNDICES:
 *   - idx_reservation_status:     optimiza las queries del scheduler que filtran por status.
 *   - idx_reservation_expires_at: optimiza la búsqueda de reservas expiradas por fecha.
 */
@Entity
@Table(
    name = "reservation",
    indexes = {
        @Index(name = "idx_reservation_status", columnList = "status"),
        @Index(name = "idx_reservation_expires_at", columnList = "expires_at")
    }
)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Conductor que realizó la reserva */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    /** Cochera reservada */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_space_id", nullable = false)
    private ParkingSpace parkingSpace;

    /** Estado actual de la reserva (ver enum ReservationStatus) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;

    /**
     * Momento en que el DRIVER hizo check-in (llegó a la cochera).
     * Es null mientras la reserva está en PENDING.
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * Momento en que el DRIVER hizo check-out (liberó la cochera).
     * Es null mientras la reserva no ha terminado (PENDING o ACTIVE).
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * Momento en que la reserva fue creada.
     * Asignado automáticamente por Hibernate con @CreationTimestamp.
     */
    @CreationTimestamp
    @Column(name = "reserved_at", updatable = false)
    private LocalDateTime reservedAt;

    /**
     * Tiempo límite para que el DRIVER haga check-in.
     * Se calcula como reservedAt + 15 minutos en el service al crear la reserva.
     * Si el DRIVER no hace check-in antes de expiresAt, el scheduler cambia
     * el status a EXPIRED y libera la cochera.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** Fecha y hora de creación del registro en BD */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ==================== Enum de Estados ====================

    public enum ReservationStatus {
        /** Reserva creada, esperando check-in del DRIVER (máx. 15 min) */
        PENDING,
        /** El DRIVER hizo check-in y está usando la cochera */
        ACTIVE,
        /** El DRIVER hizo check-out o la reserva se completó normalmente */
        FINISHED,
        /** La reserva expiró sin check-in o fue cancelada por el DRIVER */
        EXPIRED
    }
}
