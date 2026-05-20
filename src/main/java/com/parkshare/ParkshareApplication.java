package com.parkshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la aplicación ParkShare.
 * Marketplace de estacionamiento on-demand en Lima, Perú.
 *
 * @EnableScheduling habilita la ejecución de métodos @Scheduled usados por
 * ReservationScheduler para detectar reservas expiradas y enviar notificaciones.
 */
@SpringBootApplication
@EnableScheduling
public class ParkshareApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParkshareApplication.class, args);
    }
}
