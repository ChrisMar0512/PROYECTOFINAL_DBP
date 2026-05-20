package com.parkshare.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para la actualización parcial de una cochera existente.
 * Todos los campos son opcionales — solo se actualizan los que no sean null.
 * Recibido como JSON en el campo 'data' del @RequestPart del endpoint PUT /api/parking-spaces/{id}.
 */
@Data
public class UpdateParkingSpaceRequest {

    /** Nuevo título de la cochera (null = sin cambios) */
    private String title;

    /** Nueva descripción (null = sin cambios) */
    private String description;

    /** Nueva dirección (null = sin cambios) */
    private String address;

    /** Nuevo precio por hora en soles (null = sin cambios) */
    private BigDecimal pricePerHour;

    /** Nueva latitud GPS (null = sin cambios) */
    private Double latitude;

    /** Nueva longitud GPS (null = sin cambios) */
    private Double longitude;

    /** Nuevas IDs de features — reemplaza la lista completa si no es null */
    private List<Long> featureIds;
}
