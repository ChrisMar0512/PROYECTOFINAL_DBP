package com.parkshare.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de respuesta para una cochera.
 * Incluye coordenadas extraídas del objeto Point de PostGIS,
 * el nombre del host y la cantidad de favoritos.
 */
@Data
public class ParkingSpaceResponse {

    private Long id;
    private String title;
    private String description;
    private String address;
    private BigDecimal pricePerHour;
    private String status;

    /** Latitud extraída del Point de PostGIS (eje Y en WGS84) */
    private Double latitude;

    /** Longitud extraída del Point de PostGIS (eje X en WGS84) */
    private Double longitude;

    /** URL segura (HTTPS) de la foto almacenada en Cloudinary */
    private String photoUrl;

    /** Lista de nombres de amenidades/features de la cochera */
    private List<String> features;

    /** Nombre del propietario de la cochera */
    private String hostName;

    /** Cantidad de usuarios que han marcado esta cochera como favorita */
    private Integer favoritesCount;
}
