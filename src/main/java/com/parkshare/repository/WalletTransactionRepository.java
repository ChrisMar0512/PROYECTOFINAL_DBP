package com.parkshare.repository;

import com.parkshare.entity.WalletTransaction;
import com.parkshare.entity.WalletTransaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Repositorio JPA para la entidad WalletTransaction.
 *
 * Incluye queries para:
 * - Historial paginado de transacciones (cronológico descendente)
 * - Historial filtrado por tipo de transacción
 * - Suma total de CHARGE por wallet (ingresos)
 * - Suma de CHARGE por rango de fecha (ingresos mensuales)
 * - Dashboard de ganancias del host
 */
@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    // ==================== Historial Paginado ====================

    /**
     * Retorna el historial de transacciones de una billetera, paginado y
     * ordenado por la más reciente primero.
     *
     * @param walletId ID de la billetera
     * @param pageable configuración de paginación (page, size)
     * @return página de transacciones
     */
    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    /**
     * Retorna el historial filtrado por tipo de transacción (TOPUP, CHARGE, REFUND).
     *
     * @param walletId ID de la billetera
     * @param type     tipo de transacción a filtrar
     * @param pageable configuración de paginación
     * @return página de transacciones del tipo indicado
     */
    Page<WalletTransaction> findByWalletIdAndTypeOrderByCreatedAtDesc(
            Long walletId,
            TransactionType type,
            Pageable pageable
    );

    // ==================== Reportes de Ingresos ====================

    /**
     * Suma el monto total de transacciones CHARGE de un wallet específico.
     * Usado para el reporte de ingresos de un usuario.
     * Retorna 0 si no hay transacciones para evitar NullPointerException.
     */
    @Query("""
        SELECT COALESCE(SUM(wt.amount), 0)
        FROM WalletTransaction wt
        WHERE wt.wallet.id = :walletId
          AND wt.type = com.parkshare.entity.WalletTransaction.TransactionType.CHARGE
        """)
    BigDecimal sumChargesByWalletId(@Param("walletId") Long walletId);

    /**
     * Suma el monto de CHARGE en un rango de fechas (p.ej. mes actual).
     */
    @Query("""
        SELECT COALESCE(SUM(wt.amount), 0)
        FROM WalletTransaction wt
        WHERE wt.wallet.id = :walletId
          AND wt.type = com.parkshare.entity.WalletTransaction.TransactionType.CHARGE
          AND wt.createdAt >= :from
          AND wt.createdAt < :to
        """)
    BigDecimal sumChargesByWalletIdAndDateRange(
            @Param("walletId") Long walletId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Cuenta el total de transacciones de un wallet.
     */
    long countByWalletId(Long walletId);

    // ==================== Dashboard del Host (compatibilidad) ====================

    /**
     * Suma el monto total de transacciones de tipo CHARGE vinculadas a
     * reservas de las cocheras de un host específico.
     *
     * Usado en el dashboard: totalEarnings del host.
     * Retorna 0 si no hay transacciones para evitar NullPointerException.
     */
    @Query("""
        SELECT COALESCE(SUM(wt.amount), 0)
        FROM WalletTransaction wt
        WHERE wt.reservation.parkingSpace.host.id = :hostId
          AND wt.type = com.parkshare.entity.WalletTransaction.TransactionType.CHARGE
        """)
    BigDecimal sumEarningsByHostId(@Param("hostId") Long hostId);
}
