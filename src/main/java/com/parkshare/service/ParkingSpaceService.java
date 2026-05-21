package com.parkshare.service;

import com.parkshare.dto.*;
import com.parkshare.dto.HostDashboardResponse.ReservationSummary;
import com.parkshare.entity.*;
import com.parkshare.entity.ParkingSpace.ParkingSpaceStatus;
import com.parkshare.exception.ResourceNotFoundException;
import com.parkshare.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio central para la gestión de cocheras (ParkingSpace) en ParkShare.
 *
 * Responsabilidades:
 * - CRUD de cocheras con control de acceso por host
 * - Integración con Cloudinary para gestión de fotos
 * - Búsqueda espacial por radio usando PostGIS via ParkingSpaceRepository
 * - Gestión de favoritos de conductores
 * - Dashboard de métricas para el host
 * - Emisión de eventos WebSocket al cambiar disponibilidad
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParkingSpaceService {

    private final ParkingSpaceRepository parkingSpaceRepository;
    private final FeatureRepository featureRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final ReviewRepository reviewRepository;
    private final CloudinaryService cloudinaryService;
    private final SimpMessagingTemplate messagingTemplate;

    // ==================== CRUD de Cocheras ====================

    /**
     * Crea una nueva cochera para el host autenticado.
     * Si se provee foto, la sube a Cloudinary y guarda URL + publicId.
     * Construye el Point geográfico con SRID 4326 (WGS84/GPS).
     *
     * @param request datos de la cochera a crear
     * @param photo   foto opcional de la cochera
     * @return respuesta con los datos de la cochera creada
     */
    public ParkingSpaceResponse createParkingSpace(
            CreateParkingSpaceRequest request,
            MultipartFile photo
    ) {
        User host = getAuthenticatedUser();

        ParkingSpace space = new ParkingSpace();
        space.setHost(host);
        space.setTitle(request.getTitle());
        space.setDescription(request.getDescription());
        space.setAddress(request.getAddress());
        space.setPricePerHour(request.getPricePerHour());

        // Construir el punto geográfico WGS84 — longitud=X, latitud=Y
        space.setLocation(buildPoint(request.getLongitude(), request.getLatitude()));

        // Subir foto a Cloudinary si se proveyó
        if (photo != null && !photo.isEmpty()) {
            String[] uploadResult = cloudinaryService.uploadImageWithPublicId(photo);
            space.setPhotoUrl(uploadResult[0]);
            space.setCloudinaryPublicId(uploadResult[1]);
        }

        // Asociar features seleccionadas
        if (request.getFeatureIds() != null && !request.getFeatureIds().isEmpty()) {
            Set<Feature> features = new HashSet<>(
                    featureRepository.findAllById(request.getFeatureIds())
            );
            space.setFeatures(features);
        }

        ParkingSpace saved = parkingSpaceRepository.save(space);
        log.info("Cochera creada: id={}, host={}", saved.getId(), host.getEmail());
        return mapToResponse(saved);
    }

    /**
     * Actualiza los datos de una cochera existente.
     * Solo el host dueño puede actualizar su cochera.
     * Si se provee nueva foto, elimina la anterior de Cloudinary y sube la nueva.
     *
     * @param id      ID de la cochera a actualizar
     * @param request datos a actualizar (campos null = sin cambios)
     * @param photo   nueva foto opcional
     * @return respuesta con los datos actualizados
     */
    public ParkingSpaceResponse updateParkingSpace(
            Long id,
            UpdateParkingSpaceRequest request,
            MultipartFile photo
    ) {
        ParkingSpace space = findSpaceAndVerifyOwnership(id);

        // Actualizar solo campos no nulos
        if (request.getTitle() != null)        space.setTitle(request.getTitle());
        if (request.getDescription() != null)  space.setDescription(request.getDescription());
        if (request.getAddress() != null)      space.setAddress(request.getAddress());
        if (request.getPricePerHour() != null) space.setPricePerHour(request.getPricePerHour());

        // Actualizar coordenadas si se proveyeron ambas
        if (request.getLatitude() != null && request.getLongitude() != null) {
            space.setLocation(buildPoint(request.getLongitude(), request.getLatitude()));
        }

        // Actualizar features si se proveyó lista nueva
        if (request.getFeatureIds() != null) {
            Set<Feature> features = new HashSet<>(
                    featureRepository.findAllById(request.getFeatureIds())
            );
            space.setFeatures(features);
        }

        // Gestión de foto: eliminar anterior y subir nueva
        if (photo != null && !photo.isEmpty()) {
            // Eliminar la foto anterior de Cloudinary si existe
            if (space.getCloudinaryPublicId() != null) {
                cloudinaryService.deleteImage(space.getCloudinaryPublicId());
                log.info("Foto anterior eliminada de Cloudinary: {}", space.getCloudinaryPublicId());
            }
            // Subir la nueva foto
            String[] uploadResult = cloudinaryService.uploadImageWithPublicId(photo);
            space.setPhotoUrl(uploadResult[0]);
            space.setCloudinaryPublicId(uploadResult[1]);
        }

        ParkingSpace updated = parkingSpaceRepository.save(space);
        log.info("Cochera actualizada: id={}", updated.getId());
        return mapToResponse(updated);
    }

    /**
     * Cambia el estado de disponibilidad de una cochera.
     * Solo el host dueño puede cambiar el estado.
     * Emite un evento WebSocket al topic /topic/parking/{id}/availability
     * para que los clientes conectados sean notificados en tiempo real.
     *
     * @param id        ID de la cochera
     * @param newStatus nuevo estado (AVAILABLE, RESERVED, OCCUPIED)
     * @return respuesta actualizada
     */
    public ParkingSpaceResponse updateAvailability(Long id, ParkingSpaceStatus newStatus) {
        ParkingSpace space = findSpaceAndVerifyOwnership(id);
        ParkingSpaceStatus oldStatus = space.getStatus();

        space.setStatus(newStatus);
        ParkingSpace updated = parkingSpaceRepository.save(space);

        // Emitir evento WebSocket para notificar clientes en tiempo real
        messagingTemplate.convertAndSend(
                "/topic/parking/" + id + "/availability",
                newStatus.name()
        );

        log.info("Disponibilidad de cochera {} cambiada: {} -> {}", id, oldStatus, newStatus);
        return mapToResponse(updated);
    }

    // ==================== Búsqueda ====================

    /**
     * Busca cocheras disponibles dentro de un radio.
     * Usa PostGIS ST_DWithin con ::geography para búsqueda precisa en metros.
     *
     * @param lat          latitud del punto central
     * @param lng          longitud del punto central
     * @param radiusMeters radio en metros (default 1000 m = 1 km)
     * @return lista de cocheras disponibles dentro del radio
     */
    @Transactional(readOnly = true)
    public List<ParkingSpaceResponse> searchNearby(double lat, double lng, double radiusMeters) {
        log.info("Búsqueda de cocheras: lat={}, lng={}, radio={}m", lat, lng, radiusMeters);
        return parkingSpaceRepository.findNearby(lat, lng, radiusMeters)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== Favoritos ====================

    /**
     * Agrega una cochera a los favoritos del DRIVER autenticado.
     *
     * @param spaceId ID de la cochera a agregar como favorito
     */
    public void addFavorite(Long spaceId) {
        User driver = getAuthenticatedUser();
        ParkingSpace space = findSpaceById(spaceId);
        space.getFavoritedBy().add(driver);
        parkingSpaceRepository.save(space);
        log.info("Favorito agregado: driver={}, cochera={}", driver.getEmail(), spaceId);
    }

    /**
     * Elimina una cochera de los favoritos del DRIVER autenticado.
     *
     * @param spaceId ID de la cochera a eliminar de favoritos
     */
    public void removeFavorite(Long spaceId) {
        User driver = getAuthenticatedUser();
        ParkingSpace space = findSpaceById(spaceId);
        space.getFavoritedBy().remove(driver);
        parkingSpaceRepository.save(space);
        log.info("Favorito eliminado: driver={}, cochera={}", driver.getEmail(), spaceId);
    }

    /**
     * Retorna las cocheras marcadas como favoritas por el DRIVER autenticado.
     *
     * @return lista de cocheras favoritas del driver
     */
    @Transactional(readOnly = true)
    public List<ParkingSpaceResponse> getFavorites() {
        User driver = getAuthenticatedUser();
        return parkingSpaceRepository.findAll()
                .stream()
                .filter(space -> space.getFavoritedBy().contains(driver))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== Mis Cocheras (Host) ====================

    /**
     * Retorna todas las cocheras publicadas por el HOST autenticado.
     *
     * @return lista de cocheras del host autenticado
     */
    @Transactional(readOnly = true)
    public List<ParkingSpaceResponse> getMyParkingSpaces() {
        User host = getAuthenticatedUser();
        return parkingSpaceRepository.findByHostId(host.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== Dashboard ====================

    /**
     * Genera el dashboard de métricas para un host.
     * Agrega: total de cocheras, reservas completadas, ganancias totales,
     * rating promedio y las últimas 5 reservas recientes.
     *
     * @param hostId ID del host (debe ser el autenticado en el controlador)
     * @return HostDashboardResponse con todas las métricas
     */
    @Transactional(readOnly = true)
    public HostDashboardResponse getHostDashboard(Long hostId) {
        HostDashboardResponse dashboard = new HostDashboardResponse();

        // Total de cocheras del host
        dashboard.setTotalSpaces(parkingSpaceRepository.countByHostId(hostId));

        // Total de reservas con estado FINISHED en cocheras del host
        dashboard.setTotalReservationsCompleted(
                reservationRepository.countFinishedByHostId(hostId)
        );

        // Ganancias totales (suma de WalletTransaction tipo CHARGE)
        BigDecimal earnings = walletTransactionRepository.sumEarningsByHostId(hostId);
        dashboard.setTotalEarnings(earnings != null ? earnings : BigDecimal.ZERO);

        // Rating promedio de todas las reviews en cocheras del host
        dashboard.setAverageRating(reviewRepository.averageRatingByHostId(hostId));

        // Últimas 5 reservas recientes
        List<ReservationSummary> recentReservations = reservationRepository
                .findRecentByHostId(hostId, 5)
                .stream()
                .map(this::mapToReservationSummary)
                .collect(Collectors.toList());
        dashboard.setRecentReservations(recentReservations);

        log.info("Dashboard generado para host id={}", hostId);
        return dashboard;
    }

    /**
     * Retorna el detalle de una cochera específica por su ID.
     *
     * @param id ID de la cochera
     * @return DTO con la información de la cochera
     */
    @Transactional(readOnly = true)
    public ParkingSpaceResponse getParkingSpaceById(Long id) {
        ParkingSpace space = findSpaceById(id);
        return mapToResponse(space);
    }

    /**
     * Elimina una cochera del sistema. Solo el HOST dueño puede eliminarla.
     * Si la cochera tiene reservas activas o pendientes, se lanza una excepción.
     *
     * @param id ID de la cochera a eliminar
     */
    public void deleteParkingSpace(Long id) {
        ParkingSpace space = findSpaceAndVerifyOwnership(id);

        // Verificar si la cochera tiene reservas activas o pendientes
        long activeReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getParkingSpace().getId().equals(id) && 
                        (r.getStatus() == Reservation.ReservationStatus.PENDING || 
                         r.getStatus() == Reservation.ReservationStatus.ACTIVE))
                .count();

        if (activeReservations > 0) {
            throw new IllegalStateException("No se puede eliminar la cochera porque tiene reservas activas o pendientes.");
        }

        // Eliminar referencias en favoritos primero
        space.getFavoritedBy().clear();
        parkingSpaceRepository.save(space);

        // Eliminar foto de Cloudinary si existe
        if (space.getCloudinaryPublicId() != null) {
            cloudinaryService.deleteImage(space.getCloudinaryPublicId());
            log.info("Foto de cochera eliminada de Cloudinary: {}", space.getCloudinaryPublicId());
        }

        parkingSpaceRepository.delete(space);
        log.info("Cochera de baja id={}", id);
    }

    // ==================== Métodos Auxiliares ====================

    /**
     * Obtiene el usuario autenticado desde el SecurityContextHolder.
     */
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado: " + email));
    }

    /**
     * Busca una cochera por ID o lanza excepción si no existe.
     */
    private ParkingSpace findSpaceById(Long id) {
        return parkingSpaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cochera no encontrada con id: " + id));
    }

    /**
     * Busca una cochera y verifica que el host autenticado sea el dueño.
     * Lanza AccessDeniedException si no es el dueño.
     */
    private ParkingSpace findSpaceAndVerifyOwnership(Long id) {
        ParkingSpace space = findSpaceById(id);
        User currentUser = getAuthenticatedUser();

        if (!space.getHost().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "No tienes permiso para modificar esta cochera"
            );
        }
        return space;
    }

    /**
     * Construye un Point de JTS con SRID 4326 (WGS84).
     * Nota: en WGS84, X = longitud, Y = latitud.
     *
     * @param longitude longitud GPS (eje X)
     * @param latitude  latitud GPS (eje Y)
     * @return Point con SRID 4326
     */
    private Point buildPoint(double longitude, double latitude) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        // SRID 4326 = WGS84, el sistema de referencia estándar de GPS
        point.setSRID(4326);
        return point;
    }

    /**
     * Mapea una entidad ParkingSpace a su DTO de respuesta.
     * Extrae coordenadas del objeto Point de PostGIS (X=lng, Y=lat).
     */
    private ParkingSpaceResponse mapToResponse(ParkingSpace space) {
        ParkingSpaceResponse response = new ParkingSpaceResponse();
        response.setId(space.getId());
        response.setTitle(space.getTitle());
        response.setDescription(space.getDescription());
        response.setAddress(space.getAddress());
        response.setPricePerHour(space.getPricePerHour());
        response.setStatus(space.getStatus().name());
        response.setPhotoUrl(space.getPhotoUrl());
        response.setHostName(space.getHost().getName());
        response.setFavoritesCount(space.getFavoritedBy().size());

        // Extraer coordenadas del Point de PostGIS
        if (space.getLocation() != null) {
            response.setLatitude(space.getLocation().getY());   // Y = latitud
            response.setLongitude(space.getLocation().getX());  // X = longitud
        }

        // Mapear features a lista de nombres
        List<String> featureNames = space.getFeatures()
                .stream()
                .map(Feature::getName)
                .collect(Collectors.toList());
        response.setFeatures(featureNames);

        return response;
    }

    /**
     * Mapea una Reservation a su resumen para el dashboard del host.
     */
    private ReservationSummary mapToReservationSummary(Reservation reservation) {
        ReservationSummary summary = new ReservationSummary();
        summary.setReservationId(reservation.getId());
        summary.setDriverName(reservation.getDriver().getName());
        summary.setParkingSpaceTitle(reservation.getParkingSpace().getTitle());
        summary.setStatus(reservation.getStatus().name());
        summary.setCreatedAt(reservation.getCreatedAt());
        return summary;
    }
}
