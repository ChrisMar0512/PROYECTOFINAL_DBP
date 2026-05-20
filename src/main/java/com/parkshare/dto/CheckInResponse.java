package com.parkshare.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de respuesta tras un check-in exitoso.
 */
@Data
public class CheckInResponse {

    /** Mensaje de confirmación */
    private String message;

    /** Momento exacto del check-in */
    private LocalDateTime timestamp;
}
