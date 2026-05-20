package com.parkshare.repository;

import com.parkshare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad User.
 * Proporciona operaciones CRUD estándar y búsqueda por email
 * necesaria para la autenticación con Spring Security.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por su dirección de email.
     * Usado por UserDetailsService durante el proceso de autenticación.
     *
     * @param email el email a buscar
     * @return Optional con el usuario si existe, vacío en caso contrario
     */
    Optional<User> findByEmail(String email);
}
