package com.parkshare.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para la API de ParkShare.
 * Centraliza el manejo de errores y produce respuestas JSON consistentes
 * con timestamp, status, error, message y path.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== Excepciones Personalizadas ====================

    /** 404 — recurso no encontrado en la base de datos */
    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /** 409 — recurso duplicado (email ya existe, etc.) */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(), request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /** 400 — operación inválida (reservar cochera ocupada, etc.) */
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(InvalidOperationException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage(), request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    /** 400 — saldo insuficiente en la billetera */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "Insufficient Balance", ex.getMessage(), request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    /** 400 — QR expirado */
    @ExceptionHandler(QRCodeExpiredException.class)
    public ResponseEntity<ErrorResponse> handleQrExpired(QRCodeExpiredException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "QR Code Expired", ex.getMessage(), request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    /** 400 — QR ya utilizado */
    @ExceptionHandler(QRCodeAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleQrUsed(QRCodeAlreadyUsedException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "QR Code Already Used", ex.getMessage(), request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    /** 403 — operación no autorizada para el usuario actual */
    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedOp(UnauthorizedOperationException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage(), request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /** 400 — reserva expirada */
    @ExceptionHandler(ReservationExpiredException.class)
    public ResponseEntity<ErrorResponse> handleReservationExpired(ReservationExpiredException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "Reservation Expired", ex.getMessage(), request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    /** 409 — cochera no disponible */
    @ExceptionHandler(SpaceNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleSpaceNotAvailable(SpaceNotAvailableException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(), "Space Not Available", ex.getMessage(), request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ==================== Excepciones de Spring ====================

    /** 400 — estado inválido o regla de negocio violada */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage(), request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    /** 403 — acceso denegado por falta de permisos */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(), "Forbidden",
                "No tienes permiso para realizar esta acción", request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /** 401 — no autenticado */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthenticationException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
                "Credenciales inválidas o token expirado", request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /** 400 — errores de validación de @Valid en los DTOs */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> mensajes = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "Validation Error",
                mensajes.toString(), request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    /** 400 — body del request no legible (JSON malformado, etc.) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "El body del request no es válido o tiene formato incorrecto",
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    /** 500 — error inesperado no manejado */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                "Ocurrió un error inesperado. Por favor intenta nuevamente.",
                request.getRequestURI()
        );
        return ResponseEntity.internalServerError().body(error);
    }
}
