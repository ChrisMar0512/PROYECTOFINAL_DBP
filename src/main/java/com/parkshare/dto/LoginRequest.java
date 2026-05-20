package com.parkshare.dto;

import lombok.Data;

/**
 * DTO para la solicitud de inicio de sesión.
 * El fcmToken es opcional — se envía desde dispositivos móviles para
 * habilitar notificaciones push en el dispositivo actual.
 */
@Data
public class LoginRequest {

    private String email;
    private String password;

    /**
     * Token de Firebase Cloud Messaging del dispositivo actual.
     * Si se proporciona, se actualiza en el perfil del usuario para
     * enviarle notificaciones push desde ese momento.
     * Puede ser nulo si el login es desde web.
     */
    private String fcmToken;
}
