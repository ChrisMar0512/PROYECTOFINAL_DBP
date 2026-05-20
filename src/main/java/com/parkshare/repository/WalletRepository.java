package com.parkshare.repository;

import com.parkshare.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Wallet.
 * Proporciona búsquedas de billetera por userId y por email del usuario.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * Busca la billetera de un usuario por su ID.
     *
     * @param userId ID del usuario
     * @return Optional con la billetera si existe
     */
    Optional<Wallet> findByUserId(Long userId);

    /**
     * Busca la billetera de un usuario por su email.
     * Útil cuando se tiene el email del SecurityContextHolder pero no el ID.
     *
     * @param email email del usuario
     * @return Optional con la billetera si existe
     */
    Optional<Wallet> findByUserEmail(String email);
}
