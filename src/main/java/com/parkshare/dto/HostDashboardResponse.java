package com.parkshare.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para el dashboard del host.
 * Agrega métricas de negocio: total de cocheras, reservas completadas,
 * ganancias totales, rating promedio y últimas 5 reservas recientes.
 */
@Data
public class HostDashboardResponse {

    /** Número total de cocheras publicadas por el host */
    private long totalSpaces;

    /** Número de reservas con estado FINISHED en las cocheras del host */
    private long totalReservationsCompleted;

    /**
     * Suma total de WalletTransaction de tipo CHARGE vinculadas a reservas
     * de las cocheras del host — representa las ganancias brutas.
     */
    private BigDecimal totalEarnings;

    /**
     * Promedio del rating (1-5) de todas las reviews recibidas
     * en las cocheras del host. Null si no hay reviews aún.
     */
    private Double averageRating;

    /** Últimas 5 reservas recientes de las cocheras del host */
    private List<ReservationSummary> recentReservations;

    // ==================== DTO Anidado ====================

    /**
     * Resumen de una reserva para mostrar en el dashboard del host.
     * Incluye los datos más relevantes sin exponer toda la entidad.
     */
    @Data
    public static class ReservationSummary {

        private Long reservationId;
        private String driverName;
        private String parkingSpaceTitle;
        private String status;
        private LocalDateTime createdAt;
    }
}
