package com.parkshare.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para solicitud de check-out vía QR.
 * Recibido en POST /api/checkin/check-out.
 */
@Data
public class CheckOutRequest {

    /** UUID del código QR escaneado por el DRIVER */
    @NotBlank(message = "El código QR es obligatorio")
    private String code;
}
