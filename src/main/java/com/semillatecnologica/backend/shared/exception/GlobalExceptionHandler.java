package com.semillatecnologica.backend.shared.exception;

import com.semillatecnologica.backend.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manejador global de excepciones del sistema.
 *
 * <p>Captura todas las excepciones no controladas y las convierte en
 * respuestas ApiResponse con el formato establecido en la especificación.
 * Los detalles internos y stack traces nunca se devuelven al cliente.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja excepciones de validación de Jakarta (Bean Validation).
     *
     * @param ex Excepción de validación con los errores por campo
     * @param request Solicitud HTTP original
     * @return ApiResponse con errores de validación por campo
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, List<String>> fields = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String field = error.getField();
            String message = error.getDefaultMessage();
            fields.computeIfAbsent(field, k -> new java.util.ArrayList<>()).add(message);
        });

        String requestId = resolveRequestId(request);

        log.warn("Error de validación en {}: {}", request.getRequestURI(), fields);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("La solicitud contiene datos inválidos")
                .code("VALIDATION_ERROR")
                .fields(fields)
                .requestId(requestId)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Maneja excepciones de acceso denegado (Spring Security).
     *
     * @param ex Excepción de acceso denegado
     * @param request Solicitud HTTP original
     * @return ApiResponse con error FORBIDDEN
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        String requestId = resolveRequestId(request);

        log.warn("Acceso denegado en {}: {}", request.getRequestURI(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("No tienes permisos para realizar esta acción")
                .code("FORBIDDEN")
                .requestId(requestId)
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Maneja todas las excepciones de dominio del sistema.
     *
     * @param ex Excepción de dominio con código y mensaje
     * @param request Solicitud HTTP original
     * @return ApiResponse con el código de dominio correspondiente
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomain(
            DomainException ex,
            HttpServletRequest request) {

        String requestId = resolveRequestId(request);
        ErrorCode code = ex.getDomainCode();
        HttpStatus status = mapCodeToStatus(code);

        log.warn("Excepción de dominio [{}]: {}", code, ex.getMessage());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .code(code.name())
                .requestId(requestId)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Maneja excepciones de rate limiting (429).
     *
     * @param ex Excepción de límite superado
     * @param request Solicitud HTTP original
     * @return ApiResponse con código RATE_LIMITED
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimit(
            RateLimitExceededException ex,
            HttpServletRequest request) {

        String requestId = resolveRequestId(request);

        log.warn("Rate limit superado en {}: {}", request.getRequestURI(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .code("RATE_LIMITED")
                .requestId(requestId)
                .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    /**
     * Maneja excepciones inesperadas del sistema.
     *
     * @param ex Excepción no controlada
     * @param request Solicitud HTTP original
     * @return ApiResponse con error genérico y requestId para correlación
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(
            Exception ex,
            HttpServletRequest request) {

        String requestId = resolveRequestId(request);

        // Log conciso: sin stack trace en consola
        log.warn("[{}] {} → {}",
                requestId,
                request.getRequestURI(),
                ex.getClass().getSimpleName() + ": " + ex.getMessage());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("Error interno del servidor")
                .code("INTERNAL_ERROR")
                .requestId(requestId)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Extrae o genera el requestId de la solicitud para correlación.
     *
     * @param request Solicitud HTTP
     * @return Identificador de correlación único
     */
    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        }
        return requestId;
    }

    /**
     * Mapea un código de dominio a su status HTTP correspondiente.
     *
     * @param code Código de dominio
     * @return Status HTTP correspondiente
     */
    private HttpStatus mapCodeToStatus(ErrorCode code) {
        return switch (code) {
            case VALIDATION_ERROR, INVALID_OTP, RESET_TOKEN_EXPIRED -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED, TOKEN_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNPROCESSABLE_ENTITY -> HttpStatus.UNPROCESSABLE_ENTITY;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case INTERNAL_ERROR, INTERNAL_CONFIG_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
