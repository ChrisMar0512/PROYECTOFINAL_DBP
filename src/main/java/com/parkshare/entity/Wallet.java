package com.parkshare.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Billetera virtual de un usuario de ParkShare.
 *
 * Cada usuario (DRIVER o HOST) tiene exactamente una billetera (@OneToOne).
 * El balance se modifica EXCLUSIVAMENTE a través de WalletService, que garantiza
 * la creación simultánea de un WalletTransaction para auditoría completa.
 *
 * SEGURIDAD: las operaciones que restan del balance usan PESSIMISTIC_WRITE
 * para evitar condiciones de carrera en el saldo (double-spend).
 */
@Entity
@Table(name = "wallet")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Usuario dueño de esta billetera — relación 1:1 única */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    /**
     * Saldo actual de la billetera en soles peruanos (PEN).
     * Inicializado en BigDecimal.ZERO al crear la billetera.
     * Solo se modifica mediante las operaciones topUp, charge y refund.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    /** Última vez que se modificó el saldo — gestionado por Hibernate */
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
