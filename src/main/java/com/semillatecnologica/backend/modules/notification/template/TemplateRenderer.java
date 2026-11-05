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
            case "account-already-registered" -> renderAccountAlreadyRegistered(variables);
            case "new-raffle-available" -> renderNewRaffleAvailable(variables);
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

    private String renderAccountAlreadyRegistered(Map<String, Object> vars) {
        String userName = (String) vars.getOrDefault("userName", "Usuario");
        String loginUrl = (String) vars.getOrDefault("loginUrl", "http://localhost:5173/login");
        String forgotPasswordUrl = (String) vars.getOrDefault("forgotPasswordUrl", "http://localhost:5173/forgot-password");

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #3b82f6; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { padding: 25px; background-color: #f9f9f9; border: 1px solid #e5e7eb; border-top: none; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #3b82f6; color: white; text-decoration: none; border-radius: 6px; font-weight: bold; margin: 10px 5px; }
                    .button-secondary { display: inline-block; padding: 12px 24px; background-color: #e5e7eb; color: #374151; text-decoration: none; border-radius: 6px; font-weight: bold; margin: 10px 5px; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Ya tienes una cuenta</h1>
                    </div>
                    <div class="content">
                        <p>Hola %s,</p>
                        <p>Recibimos una solicitud para registrar una nueva cuenta con este correo en <strong>TiendaRifas</strong>.</p>
                        <p>Como ya tienes una cuenta registrada con nosotros, no es necesario registrarte de nuevo. Puedes iniciar sesión directamente o restablecer tu contraseña si no la recuerdas:</p>
                        <p style="text-align: center; margin: 30px 0;">
                            <a href="%s" class="button">Iniciar Sesión</a>
                            <a href="%s" class="button-secondary">Recuperar Contraseña</a>
                        </p>
                        <p style="font-size: 13px; color: #6b7280;">Si no intentaste registrarte, puedes ignorar este mensaje de forma segura. Tu cuenta continúa protegida.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 TiendaRifas. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, loginUrl, forgotPasswordUrl);
    }

    private String renderNewRaffleAvailable(Map<String, Object> vars) {
        String userName = (String) vars.getOrDefault("userName", "Cliente");
        String raffleName = (String) vars.getOrDefault("raffleName", "Nueva Rifa");
        String ticketPrice = (String) vars.getOrDefault("ticketPrice", "0");
        int totalTickets = (int) vars.getOrDefault("totalTickets", 0);
        String drawDate = (String) vars.getOrDefault("drawDate", "Por definir");
        String raffleUrl = (String) vars.getOrDefault("raffleUrl", "http://localhost:5173");

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #10b981; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { padding: 25px; background-color: #f9f9f9; border: 1px solid #e5e7eb; border-top: none; }
                    .box { background: white; border: 1px solid #e5e7eb; border-radius: 6px; padding: 15px; margin: 20px 0; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #10b981; color: white; text-decoration: none; border-radius: 6px; font-weight: bold; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>¡Nueva Rifa Disponible!</h1>
                    </div>
                    <div class="content">
                        <p>Hola %s,</p>
                        <p>Nos emociona contarte que se ha publicado una nueva rifa en <strong>TiendaRifas</strong>:</p>
                        <div class="box">
                            <h3 style="margin-top:0; color: #111;">%s</h3>
                            <p style="margin: 5px 0;">🎟️ <strong>Total de boletos:</strong> %d boletos</p>
                            <p style="margin: 5px 0;">💲 <strong>Costo por boleto:</strong> $%s MXN</p>
                            <p style="margin: 5px 0;">📅 <strong>Fecha del sorteo:</strong> %s</p>
                        </div>
                        <p style="text-align: center; margin: 30px 0;">
                            <a href="%s" class="button">Ver Rifa en la Tienda</a>
                        </p>
                        <p style="font-size: 13px; color: #6b7280;">La venta de boletos comenzará muy pronto. ¡Entra a conocer el producto y prepárate para participar!</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 TiendaRifas. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, raffleName, totalTickets, ticketPrice, drawDate, raffleUrl);
    }
}
