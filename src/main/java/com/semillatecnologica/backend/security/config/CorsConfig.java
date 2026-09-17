package com.semillatecnologica.backend.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Propiedades de configuración de CORS.
 *
 * <p>Define los orígenes permitidos, métodos HTTP y headers
 * para las solicitudes cross-origin del frontend.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.cors")
public class CorsConfig {

    /**
     * Orígenes permitidos para solicitudes del frontend.
     */
    private List<String> allowedOrigins = List.of("http://localhost:3000");

    /**
     * Métodos HTTP permitidos.
     */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");

    /**
     * Headers permitidos en las solicitudes.
     */
    private List<String> allowedHeaders = List.of("*");

    /**
     * Headers que se exponen al navegador.
     */
    private List<String> exposedHeaders = List.of("X-Request-Id");

    /**
     * Si se permiten credenciales (cookies, Authorization header).
     */
    private boolean allowCredentials = true;

    /**
     * Tiempo en segundos que el navegador puede cachear la respuesta preflight.
     */
    private int maxAge = 3600;
}
