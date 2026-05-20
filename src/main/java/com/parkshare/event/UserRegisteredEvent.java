package com.parkshare.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Evento de dominio publicado cuando un nuevo usuario se registra en ParkShare.
 *
 * El {@link ParkShareEventListener} escucha este evento para disparar
 * el envío del correo de bienvenida de forma asíncrona.
 */
@Getter
public class UserRegisteredEvent extends ApplicationEvent {

    private final Long userId;
    private final String email;
    private final String name;

    /**
     * Crea una nueva instancia del evento de registro de usuario.
     *
     * @param source fuente del evento (generalmente el servicio que lo publica)
     * @param userId identificador del usuario registrado
     * @param email  correo electrónico del usuario
     * @param name   nombre del usuario
     */
    public UserRegisteredEvent(Object source, Long userId, String email, String name) {
        super(source);
        this.userId = userId;
        this.email = email;
        this.name = name;
    }
}
