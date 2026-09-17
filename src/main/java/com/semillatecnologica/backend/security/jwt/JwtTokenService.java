package com.semillatecnologica.backend.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Servicio de gestión de tokens JWT.
 *
 * <p>Se encarga de emitir, validar y extraer claims de los tokens JWT.
 * Utiliza HMAC-SHA256 para la firma. Los tokens son stateless y de vida corta.</p>
 */
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    private final JwtProperties jwtProperties;

    /**
     * Genera un access token JWT para un usuario.
     *
     * @param userId ID del usuario (sub claim)
     * @param email Email del usuario
     * @return Token JWT firmado
     */
    public String generateAccessToken(String userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (long) jwtProperties.getAccessTokenExpiration() * 1000);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("email", email)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Valida un token JWT y extrae sus claims.
     *
     * @param token Token JWT a validar
     * @return Claims del token si es válido
     * @throws JwtException si el token es inválido, expirado o malformed
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrae el ID del usuario del token (sub claim).
     *
     * @param token Token JWT
     * @return ID del usuario
     */
    public String getUserId(String token) {
        return validateToken(token).getSubject();
    }

    /**
     * Extrae el email del usuario del token.
     *
     * @param token Token JWT
     * @return Email del usuario
     */
    public String getEmail(String token) {
        return validateToken(token).get("email", String.class);
    }

    /**
     * Indica si el token ha expirado.
     *
     * @param token Token JWT
     * @return true si el token está expirado
     */
    public boolean isTokenExpired(String token) {
        try {
            return validateToken(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Extrae el ID del token (jti claim).
     *
     * @param token Token JWT
     * @return Identificador único del token
     */
    public String getTokenId(String token) {
        return validateToken(token).getId();
    }

    /**
     * Obtiene la clave de firma a partir de la propiedad secreta.
     *
     * @return SecretKey para HMAC-SHA256
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(
                        jwtProperties.getSecret().getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
