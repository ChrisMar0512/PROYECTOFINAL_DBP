package com.parkshare.controller;

import com.parkshare.dto.EarningsSummaryResponse;
import com.parkshare.dto.TopUpRequest;
import com.parkshare.dto.WalletResponse;
import com.parkshare.dto.WalletTransactionResponse;
import com.parkshare.entity.User;
import com.parkshare.entity.WalletTransaction.TransactionType;
import com.parkshare.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de la billetera virtual.
 *
 * Endpoints:
 *   GET  /api/wallet/balance                 — Saldo actual del autenticado
 *   POST /api/wallet/topup                   — Recargar saldo
 *   GET  /api/wallet/transactions             — Historial paginado de transacciones
 *   GET  /api/wallet/transactions/type/{type} — Historial filtrado por tipo
 *   GET  /api/wallet/earnings-summary         — Resumen de ganancias (solo HOST)
 *
 * Todos los endpoints requieren JWT válido.
 */
@Slf4j
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    // ==================== Saldo ====================

    /**
     * Retorna el saldo actual de la billetera del usuario autenticado.
     *
     * @return WalletResponse con userId, balance y updatedAt
     */
    @GetMapping("/balance")
    public ResponseEntity<WalletResponse> getBalance() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(walletService.getBalance(user.getId()));
    }

    // ==================== Recarga ====================

    /**
     * Recarga saldo en la billetera del usuario autenticado.
     *
     * @param request contiene el monto a recargar (debe ser positivo)
     * @return WalletResponse con el saldo actualizado
     */
    @PostMapping("/topup")
    public ResponseEntity<WalletResponse> topUp(@Valid @RequestBody TopUpRequest request) {
        User user = getAuthenticatedUser();
        WalletResponse response = walletService.topUp(user.getId(), request.getAmount());
        return ResponseEntity.ok(response);
    }

    // ==================== Historial de Transacciones ====================

    /**
     * Retorna el historial completo de transacciones del usuario autenticado,
     * paginado y ordenado por la más reciente primero.
     *
     * @param page número de página (default: 0)
     * @param size cantidad de elementos por página (default: 10)
     * @return página de transacciones
     */
    @GetMapping("/transactions")
    public ResponseEntity<Page<WalletTransactionResponse>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = getAuthenticatedUser();
        Page<WalletTransactionResponse> transactions = walletService.getTransactionHistory(
                user.getId(), PageRequest.of(page, size)
        );
        return ResponseEntity.ok(transactions);
    }

    /**
     * Retorna el historial filtrado por tipo de transacción (TOPUP, CHARGE, REFUND).
     *
     * @param type tipo de transacción a filtrar
     * @param page número de página (default: 0)
     * @param size cantidad de elementos por página (default: 10)
     * @return página de transacciones del tipo indicado
     */
    @GetMapping("/transactions/type/{type}")
    public ResponseEntity<Page<WalletTransactionResponse>> getTransactionsByType(
            @PathVariable TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = getAuthenticatedUser();
        Page<WalletTransactionResponse> transactions = walletService.getTransactionHistoryByType(
                user.getId(), type, PageRequest.of(page, size)
        );
        return ResponseEntity.ok(transactions);
    }

    // ==================== Resumen de Ganancias (HOST) ====================

    /**
     * Retorna el resumen financiero del HOST autenticado.
     * Incluye: ingresos totales, ingresos del mes actual y conteo de transacciones.
     *
     * Solo accesible para usuarios con rol HOST.
     *
     * @return EarningsSummaryResponse con métricas financieras
     */
    @GetMapping("/earnings-summary")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<EarningsSummaryResponse> getEarningsSummary() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(walletService.getEarningsSummary(user.getId()));
    }

    // ==================== Auxiliar ====================

    /**
     * Obtiene el usuario autenticado desde el SecurityContextHolder.
     */
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
}
