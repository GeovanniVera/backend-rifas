package com.semillatecnologica.backend.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuración para las cookies de autenticación.
 *
 * <p>Permite configurar los atributos de la cookie del refresh token
 * desde el application.yaml sin modificar código.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.cookie")
public class CookieProperties {

    /**
     * Nombre de la cookie del refresh token.
     */
    private String name = "refresh_token";

    /**
     * Path al que se adjunta la cookie.
     */
    private String path = "/api/auth";

    /**
     * Dominio de la cookie. Null para dominio actual.
     */
    private String domain;

    /**
     * Tiempo de vida de la cookie en segundos.
     */
    private int maxAge = 604800; // 7 días

    /**
     * Restringir cookie solo a HTTPS.
     */
    private boolean secure = true;

    /**
     * Prevenir acceso desde JavaScript.
     */
    private boolean httpOnly = true;

    /**
     * Política de envío cross-site: Lax, Strict o None.
     */
    private String sameSite = "Lax";
}
