package com.semillatecnologica.backend.modules.notification.inapp.controller;

import com.semillatecnologica.backend.modules.notification.inapp.dto.InAppNotificationResponse;
import com.semillatecnologica.backend.modules.notification.inapp.service.InAppNotificationService;
import com.semillatecnologica.backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de notificaciones in-app.
 *
 * <p>Expone los endpoints que el frontend consume para listar,
 * marcar como leídas y eliminar notificaciones.</p>
 *
 * <p>Todos los endpoints operan sobre el usuario autenticado.
 * No hay acceso a notificaciones de otros usuarios.</p>
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Notificaciones in-app del usuario autenticado")
public class NotificationController {

    private final InAppNotificationService inAppNotificationService;

    /**
     * Lista las notificaciones del usuario autenticado.
     *
     * @param type Filtro por tipo (SECURITY, WORKFLOW, etc.) — opcional
     * @param unreadOnly Si es true, solo retorna no leídas
     * @param pageable Parámetros de paginación
     * @param authentication Contexto de autenticación del usuario
     * @return Página de notificaciones
     */
    @GetMapping
    @Operation(summary = "Listar notificaciones",
               description = "Lista las notificaciones del usuario autenticado con filtros y paginación")
    public ResponseEntity<ApiResponse<Page<InAppNotificationResponse>>> list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            Pageable pageable,
            Authentication authentication) {

        String userId = authentication.getName();
        Page<InAppNotificationResponse> notifications =
                inAppNotificationService.listByUser(userId, type, unreadOnly, pageable);

        return ResponseEntity.ok(ApiResponse.ok("Notificaciones obtenidas", notifications));
    }

    /**
     * Retorna el conteo de notificaciones no leídas.
     *
     * @param authentication Contexto de autenticación del usuario
     * @return Cantidad de no leídas
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Conteo de no leídas",
               description = "Retorna la cantidad de notificaciones no leídas para el badge del header")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> unreadCount(Authentication authentication) {
        String userId = authentication.getName();
        long count = inAppNotificationService.countUnread(userId);

        return ResponseEntity.ok(ApiResponse.ok("Conteo obtenido", new UnreadCountResponse(count)));
    }

    /**
     * Marca una notificación como leída.
     *
     * @param id ID de la notificación
     * @param authentication Contexto de autenticación del usuario
     * @return true si se marcó correctamente
     */
    @PatchMapping("/{id}/read")
    @Operation(summary = "Marcar como leída",
               description = "Marca una notificación específica como leída")
    public ResponseEntity<ApiResponse<Boolean>> markAsRead(
            @PathVariable String id,
            Authentication authentication) {

        String userId = authentication.getName();
        boolean marked = inAppNotificationService.markAsRead(id, userId);

        if (!marked) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Notificación no encontrada", "NOT_FOUND"));
        }

        return ResponseEntity.ok(ApiResponse.ok("Notificación marcada como leída", true));
    }

    /**
     * Marca todas las notificaciones no leídas como leídas.
     *
     * @param authentication Contexto de autenticación del usuario
     * @return Cantidad de notificaciones marcadas
     */
    @PatchMapping("/read-all")
    @Operation(summary = "Marcar todas como leídas",
               description = "Marca todas las notificaciones no leídas del usuario como leídas")
    public ResponseEntity<ApiResponse<MarkAllReadResponse>> markAllAsRead(Authentication authentication) {
        String userId = authentication.getName();
        int count = inAppNotificationService.markAllAsRead(userId);

        return ResponseEntity.ok(ApiResponse.ok(
                count + " notificaciones marcadas como leídas",
                new MarkAllReadResponse(count)));
    }

    /**
     * Elimina una notificación.
     *
     * @param id ID de la notificación
     * @param authentication Contexto de autenticación del usuario
     * @return true si se eliminó correctamente
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar notificación",
               description = "Elimina una notificación del usuario")
    public ResponseEntity<ApiResponse<Boolean>> delete(
            @PathVariable String id,
            Authentication authentication) {

        String userId = authentication.getName();
        boolean deleted = inAppNotificationService.delete(id, userId);

        if (!deleted) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Notificación no encontrada", "NOT_FOUND"));
        }

        return ResponseEntity.ok(ApiResponse.ok("Notificación eliminada", true));
    }

    /**
     * DTO para conteo de no leídas.
     */
    public record UnreadCountResponse(long count) {}

    /**
     * DTO para respuesta de mark-all-as-read.
     */
    public record MarkAllReadResponse(int markedCount) {}
}
