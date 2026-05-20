package com.parkshare.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Evento de dominio publicado cuando una reserva expira sin que el conductor
 * haya realizado el checkout.
 *
 * El {@link ParkShareEventListener} escucha este evento para registrar
 * en los logs que la reserva ha vencido y liberar recursos si fuera necesario.
 */
@Getter
public class ReservationExpiredEvent extends ApplicationEvent {

    private final Long reservationId;
    private final Long parkingSpaceId;

    /**
     * Crea una nueva instancia del evento de reserva expirada.
     *
     * @param source         fuente del evento
     * @param reservationId  identificador de la reserva que expiró
     * @param parkingSpaceId identificador de la cochera asociada
     */
    public ReservationExpiredEvent(Object source, Long reservationId, Long parkingSpaceId) {
        super(source);
        this.reservationId = reservationId;
        this.parkingSpaceId = parkingSpaceId;
    }
}
