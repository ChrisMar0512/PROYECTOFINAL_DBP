package com.parkshare.repository;

import com.parkshare.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Feature (amenidades de cocheras).
 * Proporciona operaciones CRUD estándar y búsqueda por nombre.
 */
@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {

    /**
     * Busca una feature por su nombre exacto.
     * Usado en el DataSeeder para evitar duplicados.
     */
    Optional<Feature> findByName(String name);

    /** Verifica si existe al menos una feature — usado por el DataSeeder */
    boolean existsByName(String name);
}
