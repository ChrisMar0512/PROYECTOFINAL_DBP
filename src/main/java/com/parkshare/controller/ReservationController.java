package com.parkshare.controller;

import com.parkshare.dto.CreateReservationRequest;
import com.parkshare.dto.ReservationResponse;
import com.parkshare.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de reservas de estacionamiento.
 *
 * Endpoints:
 *   POST   /api/reservations                             — Crear reserva (DRIVER)
 *   GET    /api/reservations/my-history                  — Historial del DRIVER autenticado
 *   GET    /api/reservations/{id}                        — Detalle de una reserva
 *   DELETE /api/reservations/{id}                        — Cancelar reserva (solo PENDING)
 *   GET    /api/reservations/parking-space/{spaceId}     — Historial de una cochera (HOST)
 *
 * Todos los endpoints requieren JWT válido.
 * La autorización por rol (DRIVER/HOST) se valida en el service cuando aplica.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // ==================== Crear Reserva ====================

    /**
     * Crea una nueva reserva para el DRIVER autenticado.
     *
     * La cochera se bloquea con PESSIMISTIC_WRITE en el service para
     * evitar que dos drivers simultáneos reserven la misma cochera (race condition).
     *
     * @param request contiene el parkingSpaceId a reservar
     * @return 201 CREATED con los datos de la reserva creada
     */
    @PostMapping("/create")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request
    ) {
        ReservationResponse response = reservationService.createReservation(
                request.getParkingSpaceId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== Historial del Driver ====================

    /**
     * Retorna el historial de reservas del DRIVER autenticado,
     * ordenado por la más reciente primero.
     *
     * @return lista de reservas del driver
     */
    @GetMapping("/my-driver-history")
    public ResponseEntity<List<ReservationResponse>> getMyHistory() {
        return ResponseEntity.ok(reservationService.getMyReservations());
    }

    // ==================== Detalle de Reserva ====================

    /**
     * Retorna el detalle de una reserva específica.
     *
     * @param id ID de la reserva
     * @return reserva completa con info de cochera y driver
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    // ==================== Cancelar Reserva ====================

    /**
     * Cancela una reserva PENDING del DRIVER autenticado.
     * Libera la cochera y emite evento WebSocket.
     *
     * Solo se pueden cancelar reservas en estado PENDING.
     * El DRIVER debe ser el dueño de la reserva.
     *
     * @param id ID de la reserva a cancelar
     * @return reserva actualizada con status EXPIRED
     */
    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancelReservation(id));
    }

    // ==================== Historial por Cochera (Host) ====================

    /**
     * Retorna el historial de reservas de una cochera específica.
     * Útil para que el HOST vea todas las reservas de su cochera.
     *
     * @param parkingSpaceId ID de la cochera
     * @return lista de reservas de la cochera
     */
    @GetMapping("/by-parking-space/{parkingSpaceId}")
    public ResponseEntity<List<ReservationResponse>> getReservationsByParkingSpace(
            @PathVariable Long parkingSpaceId
    ) {
        return ResponseEntity.ok(
                reservationService.getReservationsByParkingSpace(parkingSpaceId)
        );
    }
}
