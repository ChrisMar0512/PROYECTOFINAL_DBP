package com.parkshare.repository;

import com.parkshare.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio JPA para la entidad Reservation.
 *
 * Incluye queries optimizadas para:
 * - Historial de reservas del DRIVER (ordenado por más reciente)
 * - Reservas por cochera (para que el HOST vea el historial)
 * - Detección de reservas PENDING expiradas (para el scheduler)
 * - Reservas próximas a expirar (para notificaciones anticipadas Firebase)
 * - Dashboard del HOST (conteo de FINISHED y recientes)
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // ==================== Queries de Driver / Host ====================

    /**
     * Retorna todas las reservas de un DRIVER.
     *
     * @param driverId ID del conductor
     * @return lista de reservas del conductor
     */
    List<Reservation> findByDriverId(Long driverId);

    /**
     * Retorna todas las reservas de una cochera específica.
     * Usado por el HOST para ver el historial de su cochera.
     *
     * @param parkingSpaceId ID de la cochera
     * @return lista de reservas de la cochera
     */
    List<Reservation> findByParkingSpaceId(Long parkingSpaceId);

    /**
     * Retorna el historial de reservas de un DRIVER, ordenado por más reciente primero.
     * Usado en GET /api/reservations/my-history.
     *
     * @param driverId ID del conductor
     * @return lista de reservas ordenada por createdAt descendente
     */
    List<Reservation> findByDriverIdOrderByCreatedAtDesc(Long driverId);

    // ==================== Queries del Scheduler ====================

    /**
     * Busca reservas PENDING cuyo tiempo de expiración ya pasó.
     *
     * El scheduler ejecuta esta query cada 60 segundos para detectar reservas
     * donde el DRIVER no hizo check-in dentro de los 15 minutos permitidos.
     * Las reservas encontradas serán marcadas como EXPIRED y la cochera liberada.
     *
     * @param now momento actual para comparar contra expiresAt
     * @return lista de reservas pendientes que ya expiraron
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.status = com.parkshare.entity.Reservation$ReservationStatus.PENDING
          AND r.expiresAt < :now
        """)
    List<Reservation> findExpiredPendingReservations(@Param("now") LocalDateTime now);

    /**
     * Busca reservas PENDING cuyo expiresAt caiga dentro de los próximos 5 minutos.
     *
     * Usada por el scheduler de notificaciones para enviar un aviso anticipado
     * vía Firebase al DRIVER antes de que su reserva expire sin check-in.
     *
     * Rango: now <= expiresAt <= now+5min (la reserva aún no expiró pero lo hará pronto).
     *
     * @param now              momento actual
     * @param fiveMinutesLater momento actual + 5 minutos
     * @return lista de reservas que expirarán en los próximos 5 minutos
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.status = com.parkshare.entity.Reservation$ReservationStatus.PENDING
          AND r.expiresAt BETWEEN :now AND :fiveMinutesLater
        """)
    List<Reservation> findPendingReservationsExpiringIn5Minutes(
            @Param("now") LocalDateTime now,
            @Param("fiveMinutesLater") LocalDateTime fiveMinutesLater
    );

    // ==================== Queries del Dashboard del Host ====================

    /**
     * Cuenta las reservas FINISHED de las cocheras de un host.
     * Usado en el cálculo del dashboard: totalReservationsCompleted.
     */
    @Query("""
        SELECT COUNT(r) FROM Reservation r
        WHERE r.parkingSpace.host.id = :hostId
          AND r.status = com.parkshare.entity.Reservation$ReservationStatus.FINISHED
        """)
    long countFinishedByHostId(@Param("hostId") Long hostId);

    /**
     * Retorna las últimas N reservas de las cocheras de un host.
     * Usa nativeQuery con LIMIT porque JPQL estándar no soporta LIMIT directamente.
     */
    @Query(
        value = """
            SELECT r.* FROM reservation r
            JOIN parking_space ps ON r.parking_space_id = ps.id
            WHERE ps.host_id = :hostId
            ORDER BY r.created_at DESC
            LIMIT :lim
            """,
        nativeQuery = true
    )
    List<Reservation> findRecentByHostId(
            @Param("hostId") Long hostId,
            @Param("lim") int lim
    );
}
