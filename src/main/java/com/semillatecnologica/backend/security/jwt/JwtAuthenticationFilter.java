package com.semillatecnologica.backend.security.jwt;

import com.semillatecnologica.backend.security.authentication.CustomUserDetailsService;
import com.semillatecnologica.backend.shared.exception.SuspendedAccountException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autenticación JWT que se ejecuta en cada request.
 *
 * <p>Extrae el access token del header Authorization, lo valida
 * y establece el contexto de seguridad si es válido.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Filtra cada request para autenticar al usuario por JWT.
     *
     * @param request Solicitud HTTP entrante
     * @param response Respuesta HTTP
     * @param filterChain Cadena de filtros siguiente
     * @throws ServletException si hay error en el servlet
     * @throws IOException si hay error de E/O
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtTokenService.validateToken(token);
                String userId = claims.getSubject();

                try {
                    UserDetails userDetails = userDetailsService.loadUserById(userId);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                } catch (SuspendedAccountException e) {
                    log.warn("Cuenta suspendida intentando acceder: userId={}", userId);
                    writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                            "Cuenta suspendida", "ACCOUNT_SUSPENDED");
                    return;
                }

            } catch (JwtException e) {
                log.debug("Token JWT inválido: {}", e.getMessage());
            } catch (Exception e) {
                log.warn("Error al autenticar usuario: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Escribe una respuesta JSON de error directamente (antes del chain).
     */
    private void writeErrorResponse(HttpServletResponse response, int status,
                                    String message, String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String json = "{\"success\":false,\"message\":\"" + message + "\",\"code\":\"" + code + "\"}";
        response.getWriter().write(json);
    }

    /**
     * Extrae el token del header Authorization.
     *
     * @param request Solicitud HTTP
     * @return Token JWT sin el prefijo Bearer, o null si no existe
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
