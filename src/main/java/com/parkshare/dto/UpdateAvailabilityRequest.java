package com.parkshare.dto;

import com.parkshare.entity.ParkingSpace.ParkingSpaceStatus;
import lombok.Data;

/**
 * DTO para cambiar el estado de disponibilidad de una cochera.
 * Usado en PUT /api/parking-spaces/{id}/availability
 */
@Data
public class UpdateAvailabilityRequest {

    /** Nuevo estado del espacio: AVAILABLE, RESERVED u OCCUPIED */
    private ParkingSpaceStatus status;
}
