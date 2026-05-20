package com.parkshare.exception;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Estructura estándar de respuesta de error para la API de ParkShare.
 * Devuelta por GlobalExceptionHandler en todos los casos de error.
 * Incluye el path del request para facilitar debugging.
 */
@Data
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
}
