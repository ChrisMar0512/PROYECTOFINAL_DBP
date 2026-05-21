package com.parkshare;

import com.parkshare.entity.User;
import com.parkshare.repository.UserRepository;
import com.parkshare.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Sembrador de datos de prueba para ParkShare.
 * Se ejecuta automáticamente al arrancar la aplicación.
 *
 * Crea dos usuarios de prueba si no existen:
 *   - DRIVER: seeded_driver@parkshare.com / SecurePassword123 (con S/. 100 de saldo inicial)
 *   - HOST:   seeded_host@parkshare.com   / SecurePassword123
 *
 * NOTA: Solo para entornos de desarrollo/testing.
 * En producción, deshabilitar o proteger con un perfil Spring (@Profile("dev")).
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final WalletService walletService;

    private static final String SEED_CHECK_EMAIL = "admin@parkshare.com";
    private static final String DEFAULT_PASSWORD = "SecurePassword123";

    @Override
    public void run(String... args) {
        // Verificar si los datos de prueba ya fueron sembrados
        if (userRepository.findByEmail(SEED_CHECK_EMAIL).isPresent()) {
            log.info("Datos de prueba ya existen — omitiendo DataSeeder.");
            return;
        }

        log.info("Sembrando datos de prueba iniciales...");

        // --- Usuario DRIVER ---
        User driver = new User();
        driver.setName("Carlos Driver");
        driver.setEmail("seeded_driver@parkshare.com");
        driver.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        driver.setPhone("999111222");
        driver.setRole(User.Role.DRIVER);
        driver.setEnabled(true);

        User savedDriver = userRepository.save(driver);
        walletService.initializeWallet(savedDriver);
        // El conductor tiene S/. 100 de saldo inicial para poder hacer reservas
        walletService.topUp(savedDriver, new BigDecimal("100.00"));

        log.info("Usuario DRIVER creado: {} — saldo inicial S/. 100", driver.getEmail());

        // --- Usuario HOST ---
        User host = new User();
        host.setName("María Host");
        host.setEmail("seeded_host@parkshare.com");
        host.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        host.setPhone("999333444");
        host.setRole(User.Role.HOST);
        host.setEnabled(true);

        User savedHost = userRepository.save(host);
        walletService.initializeWallet(savedHost);

        log.info("Usuario HOST creado: {}", host.getEmail());

        // Crear usuario admin como marcador para detectar que ya se sembró
        User admin = new User();
        admin.setName("Admin ParkShare");
        admin.setEmail(SEED_CHECK_EMAIL);
        admin.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        admin.setPhone("999000000");
        admin.setRole(User.Role.HOST);
        admin.setEnabled(true);
        userRepository.save(admin);

        log.info("✅ Datos de prueba creados exitosamente. " +
                "Credenciales: seeded_driver@parkshare.com / {} y seeded_host@parkshare.com / {}",
                DEFAULT_PASSWORD, DEFAULT_PASSWORD);
    }
}
