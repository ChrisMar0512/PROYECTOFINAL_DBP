package com.parkshare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkshare.dto.*;
import com.parkshare.entity.ParkingSpace.ParkingSpaceStatus;
import com.parkshare.entity.User;
import com.parkshare.service.ParkingSpaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controlador REST para la gestión de cocheras (ParkingSpace).
 *
 * Endpoints públicos (requieren JWT válido):
 * GET /api/parking-spaces/search — búsqueda por radio (todos)
 * GET /api/parking-spaces/favorites — favoritos del DRIVER
 * POST /api/parking-spaces/{id}/favorites — agregar favorito (DRIVER)
 * DELETE /api/parking-spaces/{id}/favorites — quitar favorito (DRIVER)
 *
 * Endpoints de HOST:
 * POST /api/parking-spaces — crear cochera
 * PUT /api/parking-spaces/{id} — actualizar cochera
 * PUT /api/parking-spaces/{id}/availability — cambiar disponibilidad
 * GET /api/parking-spaces/mine — mis cocheras
 * GET /api/parking-spaces/dashboard — dashboard de métricas
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parking-spaces")
@RequiredArgsConstructor
public class ParkingSpaceController {

    private final ParkingSpaceService parkingSpaceService;
    private final ObjectMapper objectMapper;

    // ==================== HOST: Gestión de Cocheras ====================

    /**
     * Crea una nueva cochera. Solo accesible para usuarios con rol HOST.
     *
     * Usa @RequestPart para recibir datos JSON y foto en un mismo
     * multipart/form-data:
     * - 'data': JSON con los campos de CreateParkingSpaceRequest
     * - 'photo': archivo de imagen (opcional)
     *
     * @param dataJson JSON string del objeto CreateParkingSpaceRequest
     * @param photo    foto de la cochera (opcional)
     * @return 201 CREATED con los datos de la cochera creada
     */
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<ParkingSpaceResponse> createParkingSpace(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "photo", required = false) MultipartFile photo) throws Exception {
        CreateParkingSpaceRequest request = objectMapper.readValue(
                dataJson, CreateParkingSpaceRequest.class);
        ParkingSpaceResponse response = parkingSpaceService.createParkingSpace(request, photo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza una cochera existente. Solo el HOST dueño puede actualizarla.
     *
     * @param id       ID de la cochera a actualizar
     * @param dataJson JSON string del objeto UpdateParkingSpaceRequest
     * @param photo    nueva foto (opcional — reemplaza la anterior si se provee)
     * @return 200 OK con los datos actualizados
     */
    @PutMapping(value = "/{id}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<ParkingSpaceResponse> updateParkingSpace(
            @PathVariable Long id,
            @RequestPart("data") String dataJson,
            @RequestPart(value = "photo", required = false) MultipartFile photo) throws Exception {
        UpdateParkingSpaceRequest request = objectMapper.readValue(
                dataJson, UpdateParkingSpaceRequest.class);
        ParkingSpaceResponse response = parkingSpaceService.updateParkingSpace(id, request, photo);
        return ResponseEntity.ok(response);
    }

    /**
     * Cambia el estado de disponibilidad de una cochera.
     * Solo el HOST dueño puede cambiar el estado.
     * Emite evento WebSocket al cambiar.
     *
     * @param id      ID de la cochera
     * @param request objeto con el nuevo status
     * @return 200 OK con los datos actualizados
     */
    @PutMapping("/{id}/change-availability")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<ParkingSpaceResponse> updateAvailability(
            @PathVariable Long id,
            @RequestBody UpdateAvailabilityRequest request) {
        ParkingSpaceResponse response = parkingSpaceService.updateAvailability(
                id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna las cocheras del HOST autenticado.
     *
     * @return lista de cocheras del host
     */
    @GetMapping("/my-published-spaces")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<List<ParkingSpaceResponse>> getMyParkingSpaces() {
        return ResponseEntity.ok(parkingSpaceService.getMyParkingSpaces());
    }

    /**
     * Retorna el dashboard de métricas para el HOST autenticado.
     * Incluye: total cocheras, reservas completadas, ganancias, rating y reservas
     * recientes.
     *
     * @return HostDashboardResponse con todas las métricas
     */
    @GetMapping("/host-dashboard")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<HostDashboardResponse> getDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User host = (User) auth.getPrincipal();
        return ResponseEntity.ok(parkingSpaceService.getHostDashboard(host.getId()));
    }

    // ==================== TODOS: Búsqueda ====================

    /**
     * Busca cocheras disponibles cerca de una coordenada GPS.
     * Accesible para cualquier usuario autenticado (DRIVER o HOST).
     *
     * @param lat    latitud del punto de búsqueda
     * @param lng    longitud del punto de búsqueda
     * @param radius radio en metros (default: 1000 m)
     * @return lista de cocheras disponibles en el radio indicado
     */
    @GetMapping("/search-nearby")
    public ResponseEntity<List<ParkingSpaceResponse>> searchNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1000.0") double radius) {
        return ResponseEntity.ok(parkingSpaceService.searchNearby(lat, lng, radius));
    }

    // ==================== DRIVER: Favoritos ====================

    /**
     * Agrega una cochera a los favoritos del DRIVER autenticado.
     *
     * @param id ID de la cochera
     * @return 200 OK
     */
    @PostMapping("/{id}/add-to-favorites")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Void> addFavorite(@PathVariable Long id) {
        parkingSpaceService.addFavorite(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Elimina una cochera de los favoritos del DRIVER autenticado.
     *
     * @param id ID de la cochera
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}/remove-from-favorites")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long id) {
        parkingSpaceService.removeFavorite(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retorna la lista de cocheras favoritas del DRIVER autenticado.
     *
     * @return lista de cocheras favoritas
     */
    @GetMapping("/my-favorite-spaces")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<ParkingSpaceResponse>> getFavorites() {
        return ResponseEntity.ok(parkingSpaceService.getFavorites());
    }
}
