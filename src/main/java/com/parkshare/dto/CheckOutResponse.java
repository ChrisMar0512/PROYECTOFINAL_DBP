package com.parkshare.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO de respuesta tras un check-out exitoso.
 * Incluye el costo calculado proporcionalmente al tiempo de uso.
 */
@Data
public class CheckOutResponse {

    /** Minutos de uso del estacionamiento */
    private Long durationMinutes;

    /** Monto cobrado en soles (calculado proporcionalmente por minuto) */
    private BigDecimal totalCharged;

    /** Saldo restante en la billetera después del cobro */
    private BigDecimal remainingBalance;

    /** Nombre de la cochera utilizada */
    private String parkingSpaceName;

    /** ID de la reserva completada */
    private Long reservationId;
}
