package com.parkshare.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI / Swagger para la documentación de la API.
 * Accesible en /swagger-ui.html o /swagger-ui/index.html.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI parkShareOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ParkShare API")
                        .version("1.0")
                        .description("API REST del marketplace de estacionamiento on-demand en Lima"));
    }
}
