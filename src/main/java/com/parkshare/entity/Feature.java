package com.parkshare.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Característica o amenidad que puede tener una cochera.
 * Ejemplos: Techado, Seguridad 24h, Acceso Camionetas, Iluminación, Cámaras.
 *
 * La relación con ParkingSpace es @ManyToMany — una cochera puede tener
 * múltiples features y una feature puede aparecer en múltiples cocheras.
 */
@Entity
@Table(name = "feature")
@Data
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre único de la característica, e.g. "Techado", "Seguridad 24h" */
    @Column(unique = true, nullable = false)
    private String name;
}
