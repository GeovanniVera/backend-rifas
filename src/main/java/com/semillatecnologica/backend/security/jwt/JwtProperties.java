package com.semillatecnologica.backend.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuración del JWT.
 *
 * <p>Permite configurar la clave secreta, tiempos de expiración
 * y emisor desde el application.yaml.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    /**
     * Clave secreta para firmar los JWT. Debe tener al menos 256 bits.
     */
    private String secret = "cambia-esta-clave-en-produccion-debe-ser-larga-y-aleatoria";

    /**
     * Tiempo de vida del access token en segundos (15 minutos por defecto).
     */
    private int accessTokenExpiration = 900;

    /**
     * Emisor del token (iss claim).
     */
    private String issuer = "backend";
}
