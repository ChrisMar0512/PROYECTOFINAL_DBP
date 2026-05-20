package com.parkshare.dto;

import lombok.Data;

/**
 * DTO de respuesta al generar un código QR.
 * Contiene el UUID del QR, la imagen en base64 y el ID de la reserva.
 */
@Data
public class QRResponse {

    /** UUID del código QR — el texto codificado en la imagen */
    private String code;

    /**
     * Imagen QR en formato PNG codificada en base64.
     * El frontend puede renderizarla con: <img src="data:image/png;base64,{qrImageBase64}">
     */
    private String qrImageBase64;

    /** ID de la reserva asociada */
    private Long reservationId;
}
