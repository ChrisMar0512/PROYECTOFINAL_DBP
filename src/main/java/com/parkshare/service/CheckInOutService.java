package com.parkshare.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.parkshare.dto.CheckInResponse;
import com.parkshare.dto.CheckOutResponse;
import com.parkshare.dto.ParkingUpdateEvent;
import com.parkshare.dto.QRResponse;
import com.parkshare.dto.WalletResponse;
import com.parkshare.entity.ParkingSpace;
import com.parkshare.entity.ParkingSpace.ParkingSpaceStatus;
import com.parkshare.entity.QRCode;
import com.parkshare.entity.Reservation;
import com.parkshare.entity.Reservation.ReservationStatus;
import com.parkshare.repository.QRCodeRepository;
import com.parkshare.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInOutService {

    private final QRCodeRepository qrCodeRepository;
    private final ReservationRepository reservationRepository;
    private final WalletService walletService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public QRResponse generateQR(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada con id: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Solo se puede generar QR para reservas pendientes");
        }

        Optional<QRCode> existingQr = qrCodeRepository.findByReservationId(reservationId);
        if (existingQr.isPresent()) {
            QRCode qr = existingQr.get();
            try {
                String base64Image = generateQrBase64Image(qr.getCode());
                QRResponse response = new QRResponse();
                response.setCode(qr.getCode());
                response.setQrImageBase64(base64Image);
                response.setReservationId(reservationId);
                return response;
            } catch (Exception e) {
                throw new RuntimeException("Error al regenerar la imagen del QR", e);
            }
        }

        String code = UUID.randomUUID().toString();
        QRCode qrCode = new QRCode();
        qrCode.setReservation(reservation);
        qrCode.setCode(code);
        qrCode.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        qrCodeRepository.save(qrCode);

        try {
            String base64Image = generateQrBase64Image(code);
            QRResponse response = new QRResponse();
            response.setCode(code);
            response.setQrImageBase64(base64Image);
            response.setReservationId(reservationId);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Error al generar la imagen del QR", e);
        }
    }

    /*
     * El QR contiene solo el UUID como texto y que la validación real ocurre en el backend
     * buscando ese UUID en base de datos, nunca confiar en datos del QR sin validar.
     */
    private String generateQrBase64Image(String text) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        var bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 300, 300);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        return Base64.getEncoder().encodeToString(pngData);
    }

    @Transactional
    public CheckInResponse checkIn(String code) {
        QRCode qrCode = qrCodeRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Código QR no encontrado"));

        if (qrCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("El código QR ha expirado, solicita uno nuevo");
        }

        if (Boolean.TRUE.equals(qrCode.getUsedForCheckin())) {
            throw new IllegalStateException("Este QR ya fue utilizado para hacer check-in");
        }

        qrCode.setUsedForCheckin(true);
        Reservation reservation = qrCode.getReservation();
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setStartTime(LocalDateTime.now());
        
        ParkingSpace parkingSpace = reservation.getParkingSpace();
        parkingSpace.setStatus(ParkingSpaceStatus.OCCUPIED);

        qrCodeRepository.save(qrCode);
        reservationRepository.save(reservation);

        ParkingUpdateEvent event = new ParkingUpdateEvent(parkingSpace.getId(), ParkingSpaceStatus.OCCUPIED.name());
        messagingTemplate.convertAndSend("/topic/parking-updates", event);

        CheckInResponse response = new CheckInResponse();
        response.setMessage("Check-in exitoso");
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    @Transactional
    public CheckOutResponse checkOut(String code) {
        QRCode qrCode = qrCodeRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Código QR no encontrado"));

        Reservation reservation = qrCode.getReservation();

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("No hay una sesión activa para este espacio");
        }

        if (Boolean.TRUE.equals(qrCode.getUsedForCheckout())) {
            throw new IllegalStateException("Este QR ya fue utilizado para hacer check-out");
        }

        /*
         * El cálculo proporcional del costo se hace dividiendo la tarifa por hora entre 60.
         * Se establece mínimo 1 minuto para evitar cobrar 0 si el usuario entra y sale inmediatamente,
         * asegurando al menos el cobro de la fracción mínima de tiempo.
         */
        long minutosUsados = ChronoUnit.MINUTES.between(reservation.getStartTime(), LocalDateTime.now());
        if (minutosUsados == 0) {
            minutosUsados = 1;
        }

        BigDecimal pricePerMinute = reservation.getParkingSpace().getPricePerHour().divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal costoTotal = pricePerMinute.multiply(BigDecimal.valueOf(minutosUsados)).setScale(2, RoundingMode.HALF_UP);

        Long driverId = reservation.getDriver().getId();
        WalletResponse walletResponse = walletService.charge(driverId, costoTotal, reservation);

        qrCode.setUsedForCheckout(true);
        reservation.setEndTime(LocalDateTime.now());
        reservation.setStatus(ReservationStatus.FINISHED);

        ParkingSpace parkingSpace = reservation.getParkingSpace();
        parkingSpace.setStatus(ParkingSpaceStatus.AVAILABLE);

        qrCodeRepository.save(qrCode);
        reservationRepository.save(reservation);

        ParkingUpdateEvent event = new ParkingUpdateEvent(parkingSpace.getId(), ParkingSpaceStatus.AVAILABLE.name());
        messagingTemplate.convertAndSend("/topic/parking-updates", event);

        CheckOutResponse response = new CheckOutResponse();
        response.setDurationMinutes(minutosUsados);
        response.setTotalCharged(costoTotal);
        response.setRemainingBalance(walletResponse.getBalance());
        response.setParkingSpaceName(parkingSpace.getTitle());
        response.setReservationId(reservation.getId());
        return response;
    }
}
