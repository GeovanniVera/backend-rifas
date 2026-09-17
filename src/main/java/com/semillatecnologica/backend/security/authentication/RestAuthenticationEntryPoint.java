package com.semillatecnologica.backend.security.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semillatecnologica.backend.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Manejador de errores para requests no autenticados.
 *
 * <p>Devuelve una respuesta JSON con el formato ApiResponse
 * cuando un usuario intenta acceder a un endpoint protegido
 * sin un token válido.</p>
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Maneja el intento de acceso no autenticado.
     *
     * @param request Solicitud HTTP
     * @param response Respuesta HTTP
     * @param authException Excepción de autenticación
     * @throws IOException si hay error al escribir la respuesta
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        log.debug("Acceso no autenticado a {}: {}",
                request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message("Credenciales incorrectas o token inválido")
                .code("UNAUTHORIZED")
                .build();

        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }
}
