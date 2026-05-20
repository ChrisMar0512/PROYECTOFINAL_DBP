package com.parkshare.repository;

import com.parkshare.entity.ParkingSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad ParkingSpace.
 *
 * Incluye una consulta nativa con PostGIS para búsqueda espacial por radio:
 * - ST_DWithin con tipo ::geography convierte el radio a metros reales sobre
 *   la superficie esférica de la Tierra (más preciso que la distancia euclidiana).
 * - ST_SetSRID + ST_MakePoint construyen el punto de referencia con SRID 4326 (WGS84).
 */
@Repository
public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {

    /**
     * Busca cocheras AVAILABLE dentro de un radio en metros usando PostGIS.
     *
     * La conversión ::geography en ST_DWithin hace que el radio se interprete
     * en metros (no en grados), lo que da resultados precisos para Lima Perú.
     *
     * @param lat          latitud del punto central de búsqueda
     * @param lng          longitud del punto central de búsqueda
     * @param radiusMeters radio de búsqueda en metros (ej. 1000 = 1km)
     * @return lista de cocheras disponibles dentro del radio indicado
     */
    @Query(
        value = """
            SELECT * FROM parking_space
            WHERE status = 'AVAILABLE'
              AND ST_DWithin(
                    location,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusMeters
                  )
            """,
        nativeQuery = true
    )
    List<ParkingSpace> findNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters
    );

    /**
     * Retorna todas las cocheras publicadas por un host específico.
     *
     * @param hostId ID del usuario con rol HOST
     * @return lista de cocheras del host
     */
    List<ParkingSpace> findByHostId(Long hostId);

    /**
     * Cuenta las cocheras de un host — usado en el dashboard.
     */
    long countByHostId(Long hostId);
}
