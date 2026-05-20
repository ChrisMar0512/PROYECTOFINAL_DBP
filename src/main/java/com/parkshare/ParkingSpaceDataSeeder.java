package com.parkshare;

import com.parkshare.entity.Feature;
import com.parkshare.entity.ParkingSpace;
import com.parkshare.entity.User;
import com.parkshare.repository.FeatureRepository;
import com.parkshare.repository.ParkingSpaceRepository;
import com.parkshare.repository.UserRepository;
import com.parkshare.service.ParkingSpaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Sembrador de datos de prueba para cocheras (ParkingSpace).
 *
 * Ejecutado DESPUÉS del DataSeeder principal (@Order(2)).
 *
 * Crea si no existen:
 * 1. Features (amenidades): Techado, Seguridad 24h, Acceso Camionetas, Iluminación, Cámaras
 * 2. Tres cocheras de prueba en distritos de Lima:
 *    - Barranco   (-12.1391, -77.0214) — S/. 5.00/hora
 *    - Miraflores (-12.1219, -77.0299) — S/. 8.00/hora
 *    - San Isidro (-12.0931, -77.0465) — S/. 4.50/hora
 *
 * Las cocheras se asignan al host@parkshare.com creado por DataSeeder (@Order(1)).
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ParkingSpaceDataSeeder implements CommandLineRunner {

    private final FeatureRepository featureRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final UserRepository userRepository;

    private static final String HOST_EMAIL = "host@parkshare.com";

    /** Lista de amenidades a crear si no existen */
    private static final List<String> FEATURE_NAMES = List.of(
            "Techado",
            "Seguridad 24h",
            "Acceso Camionetas",
            "Iluminación",
            "Cámaras"
    );

    @Override
    public void run(String... args) {
        seedFeatures();
        seedParkingSpaces();
    }

    // ==================== Features ====================

    private void seedFeatures() {
        boolean anyExists = FEATURE_NAMES.stream()
                .anyMatch(featureRepository::existsByName);

        if (anyExists) {
            log.info("Features ya existen — omitiendo creación de features.");
            return;
        }

        log.info("Creando features de cocheras...");
        for (String name : FEATURE_NAMES) {
            Feature feature = new Feature();
            feature.setName(name);
            featureRepository.save(feature);
        }
        log.info("✅ Features creadas: {}", FEATURE_NAMES);
    }

    // ==================== Cocheras ====================

    private void seedParkingSpaces() {
        if (parkingSpaceRepository.count() > 0) {
            log.info("Cocheras ya existen — omitiendo creación de cocheras de prueba.");
            return;
        }

        Optional<User> hostOpt = userRepository.findByEmail(HOST_EMAIL);
        if (hostOpt.isEmpty()) {
            log.warn("Host {} no encontrado — omitiendo creación de cocheras. " +
                    "Asegúrate de que DataSeeder (@Order(1)) se ejecutó primero.", HOST_EMAIL);
            return;
        }
        User host = hostOpt.get();

        // Obtener features por nombre
        Feature techado   = getOrNull("Techado");
        Feature seguridad = getOrNull("Seguridad 24h");
        Feature camioneta = getOrNull("Acceso Camionetas");
        Feature ilum      = getOrNull("Iluminación");
        Feature camaras   = getOrNull("Cámaras");

        log.info("Creando cocheras de prueba en Lima...");

        // --- Cochera 1: Barranco (-12.1391, -77.0214) ---
        ParkingSpace barranco = new ParkingSpace();
        barranco.setHost(host);
        barranco.setTitle("Cochera Techada en Barranco");
        barranco.setDescription("Amplio estacionamiento techado con cámaras de seguridad, " +
                "a 2 cuadras del Parque Municipal de Barranco.");
        barranco.setAddress("Jr. Independencia 325, Barranco, Lima");
        barranco.setPricePerHour(new BigDecimal("5.00"));
        barranco.setLocation(buildPoint(-77.0214, -12.1391)); // lng, lat
        barranco.setFeatures(setOf(techado, camaras));
        parkingSpaceRepository.save(barranco);
        log.info("Cochera creada: 'Cochera Techada en Barranco' — S/. 5.00/h");

        // --- Cochera 2: Miraflores (-12.1219, -77.0299) ---
        ParkingSpace miraflores = new ParkingSpace();
        miraflores.setHost(host);
        miraflores.setTitle("Estacionamiento Seguro Miraflores");
        miraflores.setDescription("Estacionamiento premium en el corazón de Miraflores, " +
                "vigilancia 24 horas, acceso para camionetas y SUVs.");
        miraflores.setAddress("Av. Larco 740, Miraflores, Lima");
        miraflores.setPricePerHour(new BigDecimal("8.00"));
        miraflores.setLocation(buildPoint(-77.0299, -12.1219)); // lng, lat
        miraflores.setFeatures(setOf(seguridad, camioneta, ilum));
        parkingSpaceRepository.save(miraflores);
        log.info("Cochera creada: 'Estacionamiento Seguro Miraflores' — S/. 8.00/h");

        // --- Cochera 3: San Isidro (-12.0931, -77.0465) ---
        ParkingSpace sanIsidro = new ParkingSpace();
        sanIsidro.setHost(host);
        sanIsidro.setTitle("Parking Empresarial San Isidro");
        sanIsidro.setDescription("Cochera en zona financiera de San Isidro, " +
                "iluminación LED, ideal para ejecutivos.");
        sanIsidro.setAddress("Av. El Rosario 250, San Isidro, Lima");
        sanIsidro.setPricePerHour(new BigDecimal("4.50"));
        sanIsidro.setLocation(buildPoint(-77.0465, -12.0931)); // lng, lat
        sanIsidro.setFeatures(setOf(ilum, seguridad));
        parkingSpaceRepository.save(sanIsidro);
        log.info("Cochera creada: 'Parking Empresarial San Isidro' — S/. 4.50/h");

        log.info("✅ 3 cocheras de prueba creadas en Barranco, Miraflores y San Isidro.");
    }

    // ==================== Auxiliares ====================

    /**
     * Construye un Point con SRID 4326 (WGS84/GPS).
     * En WGS84: X = longitud, Y = latitud.
     */
    private Point buildPoint(double longitude, double latitude) {
        GeometryFactory gf = new GeometryFactory();
        Point point = gf.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        return point;
    }

    /** Busca una feature por nombre, retorna null si no existe. */
    private Feature getOrNull(String name) {
        return featureRepository.findByName(name).orElse(null);
    }

    /** Crea un Set de features filtrando nulls. */
    @SafeVarargs
    private Set<Feature> setOf(Feature... features) {
        Set<Feature> set = new HashSet<>();
        for (Feature f : features) {
            if (f != null) set.add(f);
        }
        return set;
    }
}
