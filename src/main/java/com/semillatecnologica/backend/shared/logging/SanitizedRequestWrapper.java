package com.semillatecnologica.backend.shared.logging;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

/**
 * Wrapper de HttpServletRequest que enmascara datos sensibles en el body.
 *
 * <p>Útil para logging: cuando se registra el body de una request,
 * campos como password, token o otp aparecen como "***".</p>
 */
public class SanitizedRequestWrapper extends HttpServletRequestWrapper {

    /**
     * Campos cuyo valor se enmascara al registrar.
     */
    public static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "newPassword", "confirmPassword",
            "token", "otp", "refreshToken", "accessToken",
            "secret", "apiKey", "authorization"
    );

    private final String sanitizedBody;

    public SanitizedRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.sanitizedBody = sanitizeBody(readBody(request));
    }

    @Override
    public ServletInputStream getInputStream() {
        byte[] bytes = sanitizedBody.getBytes();
        return new ServletInputStream() {
            @Override
            public int read() {
                return -1;
            }

            @Override
            public boolean isFinished() {
                return true;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                // no-op
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    /**
     * Devuelve el body sanitizado para logging.
     *
     * @return Body con valores sensibles enmascarados
     */
    public String getSanitizedBody() {
        return sanitizedBody;
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    /**
     * Enmascara valores de campos sensibles en JSON simple.
     * No es un parser JSON completo — busca "campo": "valor" y reemplaza el valor.
     */
    private String sanitizeBody(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }

        String result = body;
        for (String field : SENSITIVE_FIELDS) {
            // Busca "field": "cualquier valor" y reemplaza el valor
            result = result.replaceAll(
                    "(?i)(\"" + field + "\"\\s*:\\s*\")([^\"]*)(\")",
                    "$1***$3"
            );
        }
        return result;
    }
}
