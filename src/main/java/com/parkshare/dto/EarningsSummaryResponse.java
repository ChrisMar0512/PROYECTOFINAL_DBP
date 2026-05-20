package com.parkshare.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO de respuesta para el resumen de ganancias del HOST.
 * Retornado por GET /api/wallet/earnings-summary.
 */
@Data
public class EarningsSummaryResponse {

    /** Total de ingresos acumulados (suma de todos los CHARGE) */
    private BigDecimal totalEarned;

    /** Ingresos del mes actual */
    private BigDecimal earningsThisMonth;

    /** Cantidad total de transacciones registradas */
    private long transactionCount;
}
