package com.semillatecnologica.backend.security.cookie;

import com.semillatecnologica.backend.security.config.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Servicio para gestionar cookies HttpOnly de autenticación.
 *
 * <p>Centraliza la creación, lectura y eliminación de cookies
 * del refresh token con los atributos de seguridad configurados.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthCookieService {

    private final CookieProperties cookieProperties;

    /**
     * Crea una cookie con el refresh token y los atributos de seguridad.
     *
     * @param response Respuesta HTTP donde se agrega la cookie
     * @param token Valor del refresh token
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(cookieProperties.getName(), token);
        cookie.setPath(cookieProperties.getPath());
        cookie.setHttpOnly(cookieProperties.isHttpOnly());
        cookie.setSecure(cookieProperties.isSecure());
        cookie.setMaxAge(cookieProperties.getMaxAge());

        if (cookieProperties.getDomain() != null) {
            cookie.setDomain(cookieProperties.getDomain());
        }

        response.addCookie(cookie);
    }

    /**
     * Lee el refresh token desde las cookies de la solicitud.
     *
     * @param request Solicitud HTTP
     * @return Valor del refresh token, o null si no existe la cookie
     */
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(cookieProperties.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Elimina la cookie del refresh token (expira con Max-Age=0).
     *
     * @param response Respuesta HTTP donde se expira la cookie
     */
    public void removeRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieProperties.getName(), "");
        cookie.setPath(cookieProperties.getPath());
        cookie.setHttpOnly(cookieProperties.isHttpOnly());
        cookie.setSecure(cookieProperties.isSecure());
        cookie.setMaxAge(0);

        if (cookieProperties.getDomain() != null) {
            cookie.setDomain(cookieProperties.getDomain());
        }

        response.addCookie(cookie);
    }
}
