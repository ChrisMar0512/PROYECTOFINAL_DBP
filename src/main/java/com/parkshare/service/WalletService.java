package com.parkshare.service;

import com.parkshare.dto.EarningsSummaryResponse;
import com.parkshare.dto.WalletResponse;
import com.parkshare.dto.WalletTransactionResponse;
import com.parkshare.entity.Reservation;
import com.parkshare.entity.User;
import com.parkshare.entity.Wallet;
import com.parkshare.entity.WalletTransaction;
import com.parkshare.entity.WalletTransaction.TransactionType;
import com.parkshare.exception.InsufficientBalanceException;
import com.parkshare.exception.ResourceNotFoundException;
import com.parkshare.repository.WalletRepository;
import com.parkshare.repository.WalletTransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Servicio de billetera virtual de ParkShare.
 *
 * ┌──────────────────────────────────────────────────────────────────────────────┐
 * │  ⚠️  REGLA DE ORO — NO VIOLAR BAJO NINGUNA CIRCUNSTANCIA:                 │
 * │                                                                            │
 * │  Ningún método puede modificar wallet.balance sin crear y guardar un       │
 * │  WalletTransaction en la MISMA @Transactional.                             │
 * │                                                                            │
 * │  Si se modifica el balance sin registrar la transacción, se rompe la       │
 * │  auditoría completa del sistema: los valores de balanceAfter dejan de      │
 * │  ser confiables, el historial queda inconsistente y es imposible           │
 * │  reconstruir cómo se llegó al saldo actual.                                │
 * │                                                                            │
 * │  El campo balanceAfter en WalletTransaction existe precisamente para       │
 * │  poder auditar transacción por transacción y detectar bugs o fraudes.      │
 * └──────────────────────────────────────────────────────────────────────────────┘
 *
 * OPERACIONES:
 *   - initializeWallet: crear billetera con saldo 0 (registro de usuario)
 *   - topUp:   recargar saldo (+)
 *   - charge:  cobrar por reserva (-) con PESSIMISTIC_WRITE
 *   - refund:  reembolsar por cancelación (+)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final EntityManager entityManager;

    // ==================== Inicialización ====================

    /**
     * Crea una billetera con saldo cero para el usuario dado.
     * Llamado automáticamente al registrar un nuevo usuario.
     *
     * @param user el usuario recién registrado
     * @return la billetera creada
     */
    @Transactional
    public Wallet initializeWallet(User user) {
        // Verificar si ya tiene billetera (idempotencia)
        if (walletRepository.findByUserId(user.getId()).isPresent()) {
            log.warn("El usuario {} ya tiene billetera — omitiendo creación.", user.getEmail());
            return walletRepository.findByUserId(user.getId()).get();
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);

        Wallet saved = walletRepository.save(wallet);
        log.info("Billetera creada para usuario: {} — saldo inicial S/. 0.00", user.getEmail());
        return saved;
    }

    // ==================== Recarga de Saldo ====================

    /**
     * Agrega saldo a la billetera del usuario.
     *
     * @param userId ID del usuario
     * @param amount monto en soles a agregar (debe ser > 0)
     * @return estado actualizado de la billetera
     * @throws IllegalStateException si el monto no es positivo
     */
    @Transactional
    public WalletResponse topUp(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("El monto de recarga debe ser mayor a 0");
        }

        Wallet wallet = findWalletByUserIdOrThrow(userId);

        // Actualizar balance
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        // Registrar transacción (REGLA DE ORO: siempre junto al cambio de balance)
        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(TransactionType.TOPUP);
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setDescription("Recarga de saldo");
        walletTransactionRepository.save(tx);

        log.info("Recarga exitosa: usuario={}, monto=S/. {}, nuevoSaldo=S/. {}",
                userId, amount, newBalance);
        return mapToWalletResponse(wallet);
    }

    /**
     * Sobrecarga para compatibilidad con DataSeeder que pasa User en vez de userId.
     */
    @Transactional
    public void topUp(User user, BigDecimal amount) {
        topUp(user.getId(), amount);
    }

    // ==================== Cobro por Reserva ====================

    /**
     * Cobra al usuario por una reserva completada.
     *
     * SEGURIDAD ANTE CONCURRENCIA (PESSIMISTIC_WRITE):
     *
     * ¿Por qué se necesita PESSIMISTIC_WRITE aquí?
     * Si el mismo usuario hace check-out desde dos dispositivos simultáneamente
     * (o si un bug dispara el cobro dos veces), ambos threads podrían:
     *
     *   Thread A: lee wallet.balance = S/. 100  ✓
     *   Thread B: lee wallet.balance = S/. 100  ✓  (¡ANTES de que A persista!)
     *   Thread A: resta S/. 30 → balance = S/. 70 → guarda
     *   Thread B: resta S/. 30 → balance = S/. 70 → guarda  ← ¡COBRÓ SOLO UNA VEZ!
     *
     * En este caso el usuario pagó S/. 30 pero el balance solo bajó S/. 30 en vez de S/. 60.
     * Con PESSIMISTIC_WRITE, Thread B espera a que Thread A termine y re-lee balance = S/. 70,
     * cobrando correctamente a S/. 40.
     *
     * @param userId      ID del usuario a cobrar
     * @param amount      monto a cobrar
     * @param reservation reserva que originó el cobro
     * @return estado actualizado de la billetera
     * @throws IllegalStateException si el saldo es insuficiente
     */
    @Transactional
    public WalletResponse charge(Long userId, BigDecimal amount, Reservation reservation) {
        // PESSIMISTIC_WRITE → SELECT ... FOR UPDATE en PostgreSQL
        Wallet wallet = entityManager.find(
                Wallet.class,
                findWalletByUserIdOrThrow(userId).getId(),
                LockModeType.PESSIMISTIC_WRITE
        );

        // Validar saldo suficiente
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Saldo insuficiente para completar el pago. " +
                    "Saldo actual: S/. " + wallet.getBalance() + ", monto requerido: S/. " + amount
            );
        }

        // Restar del balance
        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);
        entityManager.merge(wallet);

        // Registrar transacción (REGLA DE ORO)
        String parkingTitle = reservation.getParkingSpace().getTitle();
        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(TransactionType.CHARGE);
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setReservation(reservation);
        tx.setDescription("Reserva en " + parkingTitle);
        walletTransactionRepository.save(tx);

        log.info("Cobro exitoso: usuario={}, monto=S/. {}, cochera={}, nuevoSaldo=S/. {}",
                userId, amount, parkingTitle, newBalance);
        return mapToWalletResponse(wallet);
    }

    // ==================== Reembolso ====================

    /**
     * Reembolsa un monto al usuario por cancelación de reserva o error.
     *
     * @param userId      ID del usuario a reembolsar
     * @param amount      monto a devolver
     * @param reservation reserva que originó el reembolso
     * @return estado actualizado de la billetera
     */
    @Transactional
    public WalletResponse refund(Long userId, BigDecimal amount, Reservation reservation) {
        Wallet wallet = findWalletByUserIdOrThrow(userId);

        // Sumar al balance
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        // Registrar transacción (REGLA DE ORO)
        String parkingTitle = reservation.getParkingSpace().getTitle();
        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(TransactionType.REFUND);
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setReservation(reservation);
        tx.setDescription("Reembolso por cancelación en " + parkingTitle);
        walletTransactionRepository.save(tx);

        log.info("Reembolso exitoso: usuario={}, monto=S/. {}, cochera={}, nuevoSaldo=S/. {}",
                userId, amount, parkingTitle, newBalance);
        return mapToWalletResponse(wallet);
    }

    // ==================== Consultas ====================

    /**
     * Retorna el saldo actual de la billetera del usuario.
     *
     * @param userId ID del usuario
     * @return estado de la billetera
     */
    @Transactional(readOnly = true)
    public WalletResponse getBalance(Long userId) {
        Wallet wallet = findWalletByUserIdOrThrow(userId);
        return mapToWalletResponse(wallet);
    }

    /**
     * Retorna el historial de transacciones del usuario, paginado.
     *
     * @param userId   ID del usuario
     * @param pageable configuración de paginación
     * @return página de transacciones mapeadas a DTO
     */
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactionHistory(Long userId, Pageable pageable) {
        Wallet wallet = findWalletByUserIdOrThrow(userId);
        return walletTransactionRepository
                .findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable)
                .map(this::mapToTransactionResponse);
    }

    /**
     * Retorna el historial filtrado por tipo de transacción.
     *
     * @param userId   ID del usuario
     * @param type     tipo de transacción (TOPUP, CHARGE, REFUND)
     * @param pageable configuración de paginación
     * @return página de transacciones del tipo indicado
     */
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactionHistoryByType(
            Long userId, TransactionType type, Pageable pageable
    ) {
        Wallet wallet = findWalletByUserIdOrThrow(userId);
        return walletTransactionRepository
                .findByWalletIdAndTypeOrderByCreatedAtDesc(wallet.getId(), type, pageable)
                .map(this::mapToTransactionResponse);
    }

    /**
     * Genera el resumen financiero del HOST autenticado.
     *
     * @param hostId ID del host
     * @return resumen con totalEarned, earningsThisMonth y transactionCount
     */
    @Transactional(readOnly = true)
    public EarningsSummaryResponse getEarningsSummary(Long hostId) {
        Wallet wallet = findWalletByUserIdOrThrow(hostId);

        EarningsSummaryResponse summary = new EarningsSummaryResponse();

        // Total de ingresos acumulados
        summary.setTotalEarned(
                walletTransactionRepository.sumChargesByWalletId(wallet.getId())
        );

        // Ingresos del mes actual
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
        summary.setEarningsThisMonth(
                walletTransactionRepository.sumChargesByWalletIdAndDateRange(
                        wallet.getId(), monthStart, monthEnd
                )
        );

        // Conteo total de transacciones
        summary.setTransactionCount(
                walletTransactionRepository.countByWalletId(wallet.getId())
        );

        log.info("Resumen de ganancias generado para host id={}", hostId);
        return summary;
    }

    // ==================== Métodos Auxiliares ====================

    /**
     * Busca la billetera de un usuario o lanza excepción si no existe.
     */
    private Wallet findWalletByUserIdOrThrow(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Billetera no encontrada para el usuario con id: " + userId
                ));
    }

    /**
     * Mapea una entidad Wallet a su DTO de respuesta.
     */
    private WalletResponse mapToWalletResponse(Wallet wallet) {
        WalletResponse response = new WalletResponse();
        response.setUserId(wallet.getUser().getId());
        response.setBalance(wallet.getBalance());
        response.setUpdatedAt(wallet.getUpdatedAt());
        return response;
    }

    /**
     * Mapea una entidad WalletTransaction a su DTO de respuesta.
     * Incluye etiquetas en español y formato de monto con signo.
     */
    private WalletTransactionResponse mapToTransactionResponse(WalletTransaction tx) {
        WalletTransactionResponse response = new WalletTransactionResponse();
        response.setId(tx.getId());
        response.setAmount(tx.getAmount());
        response.setBalanceAfter(tx.getBalanceAfter());
        response.setDescription(tx.getDescription());
        response.setCreatedAt(tx.getCreatedAt());

        // Etiqueta en español y formato del monto con signo
        switch (tx.getType()) {
            case TOPUP -> {
                response.setTypeLabel("Recarga");
                response.setAmountDisplay("+S/. " + tx.getAmount());
            }
            case CHARGE -> {
                response.setTypeLabel("Cobro por reserva");
                response.setAmountDisplay("-S/. " + tx.getAmount());
            }
            case REFUND -> {
                response.setTypeLabel("Reembolso");
                response.setAmountDisplay("+S/. " + tx.getAmount());
            }
        }

        // Datos de la reserva asociada (null para TOPUP)
        if (tx.getReservation() != null) {
            response.setReservationId(tx.getReservation().getId());
            response.setParkingSpaceName(tx.getReservation().getParkingSpace().getTitle());
        }

        return response;
    }
}
