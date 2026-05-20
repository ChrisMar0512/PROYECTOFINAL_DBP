package com.parkshare.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para solicitud de recarga de saldo.
 * Recibido en POST /api/wallet/topup.
 */
@Data
public class TopUpRequest {

    /** Monto a recargar en soles — debe ser positivo */
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    private BigDecimal amount;
}
