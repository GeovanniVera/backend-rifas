package com.semillatecnologica.backend.modules.notification.template;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Renderizador de plantillas de notificaciones.
 *
 * <p>Convierte plantillas y variables en contenido final (HTML, texto).
 * Implementación actual: plantillas hardcodeadas como Java text blocks.
 * Futuro: migrar a Thymeleaf o FreeMarker.</p>
 */
@Component
public class TemplateRenderer {

    /**
     * Renderiza una plantilla con las variables proporcionadas.
     *
     * @param templateName Nombre de la plantilla
     * @param variables Variables para la plantilla
     * @return Contenido renderizado
     */
    public String render(String templateName, Map<String, Object> variables) {
        return switch (templateName) {
            case "verification" -> renderVerification(variables);
            case "welcome" -> renderWelcome(variables);
            case "password-reset" -> renderPasswordReset(variables);
            case "password-reset-otp" -> renderPasswordResetOtp(variables);
            default -> throw new IllegalArgumentException("Plantilla no encontrada: " + templateName);
        };
    }

    private String renderVerification(Map<String, Object> vars) {
        String userName = (String) vars.getOrDefault("userName", "Usuario");
        String verificationUrl = (String) vars.getOrDefault("verificationUrl", "#");

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 4px; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Verifica tu cuenta</h1>
                    </div>
                    <div class="content">
                        <p>Hola %s,</p>
                        <p>Gracias por registrarte. Para completar tu registro, haz clic en el siguiente botón:</p>
                        <p style="text-align: center; margin: 30px 0;">
                            <a href="%s" class="button">Verificar cuenta</a>
                        </p>
                        <p>Si no puedes hacer clic en el botón, copia y pega esta URL en tu navegador:</p>
                        <p><a href="%s">%s</a></p>
                        <p>Este enlace expira en 24 horas.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 TiendaRifas. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, verificationUrl, verificationUrl, verificationUrl);
    }

    private String renderWelcome(Map<String, Object> vars) {
        String userName = (String) vars.getOrDefault("userName", "Usuario");

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>¡Bienvenido!</h1>
                    </div>
                    <div class="content">
                        <p>Hola %s,</p>
                        <p>Tu cuenta ha sido verificada exitosamente. Ya puedes acceder a todas las funcionalidades.</p>
                        <p>Si tienes alguna pregunta, no dudes en contactarnos.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 TiendaRifas. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName);
    }

    private String renderPasswordReset(Map<String, Object> vars) {
        String userName = (String) vars.getOrDefault("userName", "Usuario");
        String resetUrl = (String) vars.getOrDefault("resetUrl", "#");

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #2196F3; color: white; text-decoration: none; border-radius: 4px; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Restablece tu contraseña</h1>
                    </div>
                    <div class="content">
                        <p>Hola %s,</p>
                        <p>Recibimos una solicitud para restablecer tu contraseña. Haz clic en el siguiente botón:</p>
                        <p style="text-align: center; margin: 30px 0;">
                            <a href="%s" class="button">Restablecer contraseña</a>
                        </p>
                        <p>Si no solicitaste este cambio, puedes ignorar este email.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 TiendaRifas. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, resetUrl);
    }

    private String renderPasswordResetOtp(Map<String, Object> vars) {
        String userName = (String) vars.getOrDefault("userName", "Usuario");
        String otp = (String) vars.getOrDefault("otp", "000000");

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .otp-box { background-color: #fff; border: 2px dashed #2196F3; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #2196F3; letter-spacing: 8px; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Código de recuperación</h1>
                    </div>
                    <div class="content">
                        <p>Hola %s,</p>
                        <p>Tu código para restablecer tu contraseña es:</p>
                        <div class="otp-box">
                            <div class="otp-code">%s</div>
                        </div>
                        <p>Este código expira en 15 minutos.</p>
                        <p>Si no solicitaste este cambio, puedes ignorar este email.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 TiendaRifas. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, otp);
    }
}
