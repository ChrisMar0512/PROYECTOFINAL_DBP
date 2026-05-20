package com.parkshare.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para la solicitud de registro de nuevo usuario.
 * Todos los campos son obligatorios excepto phone.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotBlank(message = "El teléfono es obligatorio")
    private String phone;

    /**
     * Rol del usuario: "DRIVER" para conductor, "HOST" para propietario de cochera.
     */
    @NotBlank(message = "El rol es obligatorio")
    private String role;
}
