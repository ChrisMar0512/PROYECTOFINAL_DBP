package com.parkshare.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Configuración de Firebase Admin SDK.
 *
 * IMPORTANTE:
 * El archivo firebase-service-account.json se obtiene desde la consola de Firebase:
 *   Configuración del proyecto → Cuentas de servicio → Generar nueva clave privada
 *
 * SEGURIDAD: Este archivo contiene credenciales privadas y NO debe subirse al
 * repositorio git. Está listado en .gitignore para evitar exposición accidental.
 * En producción, usar variables de entorno o un gestor de secretos (e.g. GCP Secret Manager).
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path}")
    private String firebaseCredentialsPath;

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Inicializa Firebase Admin SDK con las credenciales de servicio.
     * Si Firebase ya fue inicializado (e.g. en hot-reload), no lo reinicializa
     * para evitar el error "FirebaseApp already exists".
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // Verificar si Firebase ya está inicializado (evitar doble inicialización)
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase ya estaba inicializado, reutilizando instancia existente.");
            return FirebaseApp.getInstance();
        }

        Resource resource = resourceLoader.getResource(firebaseCredentialsPath);
        try (InputStream serviceAccountStream = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK inicializado correctamente.");
            return app;
        }
    }
}
