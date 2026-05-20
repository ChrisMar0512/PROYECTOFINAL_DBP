package com.parkshare.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta con el estado actual de la billetera.
 * Retornado por las operaciones de topUp, charge, refund y getBalance.
 */
@Data
public class WalletResponse {

    /** ID del usuario dueño de la billetera */
    private Long userId;

    /** Saldo actual en soles peruanos (PEN) */
    private BigDecimal balance;

    /** Última vez que se modificó el saldo */
    private LocalDateTime updatedAt;
}
