package com.semillatecnologica.backend.security.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semillatecnologica.backend.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Manejador de errores para usuarios autenticados sin permiso suficiente.
 *
 * <p>Devuelve una respuesta JSON con el formato ApiResponse
 * cuando un usuario autenticado intenta ejecutar una operación
 * para la cual no tiene el permiso requerido.</p>
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Maneja el intento de acceso denegado.
     *
     * @param request Solicitud HTTP
     * @param response Respuesta HTTP
     * @param accessDeniedException Excepción de acceso denegado
     * @throws IOException si hay error al escribir la respuesta
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        log.debug("Acceso denegado a {}: {}",
                request.getRequestURI(), accessDeniedException.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message("No tienes permiso para realizar esta operación")
                .code("FORBIDDEN")
                .build();

        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }
}
