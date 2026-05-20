package com.parkshare.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Código QR asociado a una reserva de estacionamiento en ParkShare.
 *
 * DISEÑO — usedForCheckin / usedForCheckout separados:
 *   Los campos usedForCheckin y usedForCheckout son independientes porque permiten
 *   que un MISMO código QR físico (impreso o digital en la cochera) sirva tanto
 *   para la entrada (check-in) como para la salida (check-out) del DRIVER.
 *
 *   Si usáramos un solo campo booleano "used", habría que generar dos QRs distintos
 *   (uno para entrar y otro para salir), lo cual complica la UX tanto para el HOST
 *   que debe gestionar dos códigos como para el DRIVER que debe escanear dos.
 *
 *   Con este diseño, el HOST pega UN solo QR en la cochera y el DRIVER lo escanea
 *   al llegar (check-in) y al irse (check-out). El backend sabe en cuál de los dos
 *   estados se encuentra por la combinación de ambos campos.
 *
 * SEGURIDAD:
 *   El QR contiene únicamente un UUID aleatorio. La validación real ocurre en el
 *   backend buscando ese UUID en la base de datos. NUNCA se debe confiar en datos
 *   embebidos en el QR sin validarlos contra la BD, ya que un QR podría ser
 *   falsificado o manipulado.
 */
@Entity
@Table(name = "qr_code")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class QRCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Reserva asociada a este QR — relación 1:1 */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    /** UUID aleatorio que se codifica en la imagen QR */
    @Column(unique = true, nullable = false)
    private String code;

    /** True si el DRIVER ya usó este QR para hacer check-in (entrar a la cochera) */
    @Column(name = "used_for_checkin", nullable = false)
    private Boolean usedForCheckin = false;

    /** True si el DRIVER ya usó este QR para hacer check-out (salir de la cochera) */
    @Column(name = "used_for_checkout", nullable = false)
    private Boolean usedForCheckout = false;

    /**
     * Momento de expiración del QR.
     * Se calcula como LocalDateTime.now().plusMinutes(30) al crear.
     * Si el DRIVER no escanea antes de esta hora, debe solicitar un nuevo QR.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Momento de creación del QR */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
