package com.parkshare.repository;

import com.parkshare.entity.QRCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad QRCode.
 */
@Repository
public interface QRCodeRepository extends JpaRepository<QRCode, Long> {

    /**
     * Busca un QR por su código UUID.
     * Usado en check-in y check-out cuando el DRIVER escanea el QR.
     */
    Optional<QRCode> findByCode(String code);

    /**
     * Busca un QR por el ID de la reserva asociada.
     * Usado para verificar si ya existe un QR antes de generar uno nuevo.
     */
    Optional<QRCode> findByReservationId(Long reservationId);
}
