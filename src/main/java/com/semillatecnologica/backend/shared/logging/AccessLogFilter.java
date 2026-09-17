package com.semillatecnologica.backend.shared.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filtro que registra requests con body sanitizado.
 *
 * <p>Captura el method, URI, status y duración de cada request.
 * El body se registra solo para requests de negocio (no para archivos,
 * ni para endpoints públicos simples como health).</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("ACCESS_LOG");

    private static final String START_TIME_ATTR = "accessLogStartTime";
    private static final String SANITIZED_BODY_ATTR = "sanitizedBody";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Envolver request para poder leer body múltiples veces
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 1024);

        long startTime = System.currentTimeMillis();
        wrappedRequest.setAttribute(START_TIME_ATTR, startTime);

        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            // Leer body cacheado DESPUÉS de que el controller lo consumió
            byte[] cachedBody = wrappedRequest.getContentAsByteArray();
            if (cachedBody.length > 0) {
                String body = new String(cachedBody, StandardCharsets.UTF_8);
                String sanitized = sanitizeBody(body);
                wrappedRequest.setAttribute(SANITIZED_BODY_ATTR, sanitized);
            }

            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            String method = request.getMethod();
            String uri = request.getRequestURI();

            // No loguear health checks ni estáticos
            if (!uri.startsWith("/actuator") && !uri.startsWith("/docs")) {
                log.info("{} {} {} {}ms", method, uri, status, duration);
            }
        }
    }

    private String sanitizeBody(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        String result = body;
        for (String field : SanitizedRequestWrapper.SENSITIVE_FIELDS) {
            result = result.replaceAll(
                    "(?i)(\"" + field + "\"\\s*:\\s*\")([^\"]*)(\")",
                    "$1***$3"
            );
        }
        return result;
    }
}
