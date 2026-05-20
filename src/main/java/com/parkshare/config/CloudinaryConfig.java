package com.parkshare.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Cloudinary para ParkShare.
 *
 * Cloudinary se usa para almacenar las fotos de las cocheras.
 * Las credenciales se inyectan desde application.properties y NUNCA
 * deben estar hardcodeadas en el código fuente ni en el repositorio.
 *
 * Para obtener las credenciales:
 * 1. Crear cuenta en https://cloudinary.com
 * 2. Ir al Dashboard -> Settings -> API Keys
 * 3. Copiar Cloud Name, API Key y API Secret al application.properties
 */
@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    /**
     * Bean Cloudinary configurado con las credenciales del proyecto.
     * Se reutiliza en toda la aplicación mediante inyección de dependencias.
     */
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret
        ));
    }
}
