package com.parkshare.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para la creación de una nueva cochera.
 * Recibido como JSON en el campo 'data' del @RequestPart del endpoint POST /api/parking-spaces.
 */
@Data
public class CreateParkingSpaceRequest {

    /** Título descriptivo de la cochera, ej: "Cochera techada en Miraflores" */
    private String title;

    /** Descripción detallada del espacio */
    private String description;

    /** Dirección textual del espacio de estacionamiento */
    private String address;

    /** Precio por hora en soles (PEN) */
    private BigDecimal pricePerHour;

    /** Latitud GPS de la cochera (eje Y en WGS84) */
    private Double latitude;

    /** Longitud GPS de la cochera (eje X en WGS84) */
    private Double longitude;

    /** IDs de features/amenidades a asociar (opcional) */
    private List<Long> featureIds;
}
