package com.parkshare.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para una transacción de billetera.
 *
 * Incluye campos amigables para el frontend:
 * - typeLabel:     etiqueta en español ("Recarga", "Cobro", "Reembolso")
 * - amountDisplay: monto con signo ("+S/. 50.00" o "-S/. 25.00")
 */
@Data
public class WalletTransactionResponse {

    private Long id;

    /** Etiqueta del tipo en español: "Recarga", "Cobro por reserva", "Reembolso" */
    private String typeLabel;

    /**
     * Monto formateado con signo para mostrar en el frontend.
     * Positivo para TOPUP y REFUND (+S/. 50.00), negativo para CHARGE (-S/. 25.00).
     */
    private String amountDisplay;

    /** Monto numérico sin formato */
    private BigDecimal amount;

    /** Saldo de la billetera después de esta transacción (para auditoría) */
    private BigDecimal balanceAfter;

    /** Descripción legible de la operación, e.g. "Reserva en Cochera Miraflores Central" */
    private String description;

    /** Momento en que se realizó la transacción */
    private LocalDateTime createdAt;

    /** ID de la reserva asociada — null para recargas (TOPUP) */
    private Long reservationId;

    /** Nombre de la cochera asociada — null para recargas (TOPUP) */
    private String parkingSpaceName;
}
