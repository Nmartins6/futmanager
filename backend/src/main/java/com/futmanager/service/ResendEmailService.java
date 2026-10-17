package com.futmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ResendEmailService implements EmailService {

    private final String apiKey;
    private final String fromEmail;
    private final String frontendUrl;
    private final int passwordResetValidityMinutes;
    private final int emailVerificationValidityHours;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final int MAX_RETRIES = 3;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    public ResendEmailService(
            @Value("${resend.api-key:${RESEND_API_KEY:}}") String apiKey,
            @Value("${resend.from-email:${RESEND_FROM_EMAIL:FutManager <onboarding@resend.dev>}}") String fromEmail,
            @Value("${futmanager.frontend-url:${APP_FRONTEND_URL:http://localhost:5173}}") String frontendUrl,
            @Value("${futmanager.security.password-reset-token-validity-minutes:15}") int passwordResetValidityMinutes,
            @Value("${futmanager.security.email-verification-token-validity-hours:24}") int emailVerificationValidityHours,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.fromEmail = fromEmail != null ? fromEmail.trim() : "FutManager <onboarding@resend.dev>";
        this.frontendUrl = cleanFrontendUrl(frontendUrl);
        this.passwordResetValidityMinutes = passwordResetValidityMinutes;
        this.emailVerificationValidityHours = emailVerificationValidityHours;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        if (this.apiKey.isBlank()) {
            log.warn("[ResendEmailService] Variável RESEND_API_KEY não configurada. Os e-mails serão apenas simulados em log.");
        } else {
            log.info("[ResendEmailService] Provedor Resend configurado com remetente: {}", this.fromEmail);
        }
    }

    @Override
    public void sendEmailVerification(String toEmail, String recipientName, String rawToken) {
        String verificationUrl = String.format("%s/verify-email?token=%s", frontendUrl, rawToken);
        String subject = "⚽ Confirme seu endereço de e-mail - FutManager";
        String html = buildEmailVerificationTemplate(recipientName, verificationUrl, emailVerificationValidityHours);

        sendEmail(toEmail, subject, html);
    }

    @Override
    public void sendPasswordReset(String toEmail, String recipientName, String rawToken) {
        String resetUrl = String.format("%s/reset-password?token=%s", frontendUrl, rawToken);
        String subject = "🔒 Recuperação de senha - FutManager";
        String html = buildPasswordResetTemplate(recipientName, resetUrl, passwordResetValidityMinutes);

        sendEmail(toEmail, subject, html);
    }

    @Override
    public void sendPasswordChangedNotification(String toEmail, String recipientName) {
        String subject = "🔔 Sua senha foi alterada com sucesso - FutManager";
        String html = buildPasswordChangedTemplate(recipientName, frontendUrl);

        sendEmail(toEmail, subject, html);
    }

    /**
     * Dispara requisição HTTP à API do Resend com retry seguro e timeouts
     */
    public void sendEmail(String toEmail, String subject, String htmlContent) {
        String maskedEmail = maskEmail(toEmail);

        if (apiKey.isBlank()) {
            log.info("[SIMULAÇÃO DE E-MAIL] Para: {} | Assunto: '{}' | (Chave RESEND_API_KEY não configurada)",
                    maskedEmail, subject);
            return;
        }

        Map<String, Object> payload = Map.of(
                "from", fromEmail,
                "to", List.of(toEmail),
                "subject", subject,
                "html", htmlContent
        );

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Erro ao serializar payload de e-mail para {}: {}", maskedEmail, e.getMessage());
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESEND_API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();

                if (status >= 200 && status < 300) {
                    log.info("E-mail enviado com sucesso via Resend para {}. Assunto: '{}' (Status: {})",
                            maskedEmail, subject, status);
                    return;
                }

                if (status >= 400 && status < 500) {
                    // Erro de cliente (ex: 401 Unauthorized, 422 Unprocessable) - não adianta retentar
                    log.error("Falha na chamada ao Resend para {} (Status: {}). Resposta: {}",
                            maskedEmail, status, response.body());
                    return;
                }

                // Erro 5xx do provedor - tenta novamente após backoff
                log.warn("Servidor Resend retornou status {} na tentativa {}/{}. Aguardando retry...",
                        status, attempt, MAX_RETRIES);

            } catch (IOException e) {
                log.warn("Falha de rede ao conectar ao Resend na tentativa {}/{}: {}",
                        attempt, MAX_RETRIES, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Envio de e-mail interrompido para {}", maskedEmail);
                return;
            }

            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(500L * attempt);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        log.error("Falha definitiva ao enviar e-mail via Resend para {} após {} tentativas.", maskedEmail, MAX_RETRIES);
    }

    private String cleanFrontendUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5173";
        }
        return url.trim().replaceAll("/+$", "");
    }

    private String maskEmail(String email) {
        if (email == null) return "null";
        int at = email.indexOf('@');
        if (at > 2) {
            return email.substring(0, 2) + "***" + email.substring(at);
        }
        return "***";
    }

    // ==========================================
    // TEMPLATES HTML RESPONSIVOS COM IDENTIDADE FUTMANAGER
    // ==========================================

    private String buildEmailVerificationTemplate(String name, String verificationUrl, int validityHours) {
        String safeName = (name != null && !name.isBlank()) ? name : "Usuário";
        return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Confirme seu e-mail</title>
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 24px; color: #1e293b; }
            .container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border: 1px solid #e2e8f0; }
            .header { background: #0f172a; padding: 32px 24px; text-align: center; }
            .logo { font-size: 26px; font-weight: 800; color: #ffffff; margin: 0; }
            .logo span { color: #16a34a; }
            .content { padding: 32px 28px; line-height: 1.6; }
            .greeting { font-size: 18px; font-weight: 700; color: #0f172a; margin-top: 0; }
            .btn-wrapper { text-align: center; margin: 32px 0; }
            .btn { display: inline-block; background-color: #16a34a; color: #ffffff !important; text-decoration: none; font-weight: 700; font-size: 15px; padding: 14px 32px; border-radius: 10px; box-shadow: 0 4px 10px rgba(22, 163, 74, 0.3); }
            .note { background-color: #f8fafc; border-left: 4px solid #16a34a; padding: 14px 16px; margin: 24px 0; border-radius: 4px; font-size: 13px; color: #475569; }
            .fallback { font-size: 12px; color: #64748b; word-break: break-all; margin-top: 24px; }
            .footer { background-color: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <div class="logo">⚽ Fut<span>Manager</span></div>
            </div>
            <div class="content">
              <h1 class="greeting">Olá, %s!</h1>
              <p>Obrigado por se juntar ao <strong>FutManager</strong>, a plataforma de gestão de chamadas e frequência para escolinhas de futebol.</p>
              <p>Para ativar todos os recursos da sua conta e garantir a segurança do acesso, confirme seu endereço de e-mail clicando no botão abaixo:</p>
              <div class="btn-wrapper">
                <a href="%s" target="_blank" class="btn">Confirmar Meu E-mail</a>
              </div>
              <div class="note">
                ⏱️ Este link é válido por <strong>%d horas</strong> e pode ser utilizado apenas uma vez.
              </div>
              <p class="fallback">
                Se o botão não funcionar, copie e cole o link no seu navegador:<br>
                <a href="%s" style="color: #16a34a;">%s</a>
              </p>
              <p style="font-size: 12px; color: #64748b; margin-top: 24px;">
                Caso não tenha criado uma conta no FutManager, por favor desconsidere esta mensagem.
              </p>
            </div>
            <div class="footer">
              &copy; 2026 FutManager. Todos os direitos reservados.
            </div>
          </div>
        </body>
        </html>
        """.formatted(safeName, verificationUrl, validityHours, verificationUrl, verificationUrl);
    }

    private String buildPasswordResetTemplate(String name, String resetUrl, int validityMinutes) {
        String safeName = (name != null && !name.isBlank()) ? name : "Usuário";
        return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Redefinição de Senha</title>
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 24px; color: #1e293b; }
            .container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border: 1px solid #e2e8f0; }
            .header { background: #0f172a; padding: 32px 24px; text-align: center; }
            .logo { font-size: 26px; font-weight: 800; color: #ffffff; margin: 0; }
            .logo span { color: #16a34a; }
            .content { padding: 32px 28px; line-height: 1.6; }
            .greeting { font-size: 18px; font-weight: 700; color: #0f172a; margin-top: 0; }
            .btn-wrapper { text-align: center; margin: 32px 0; }
            .btn { display: inline-block; background-color: #2563eb; color: #ffffff !important; text-decoration: none; font-weight: 700; font-size: 15px; padding: 14px 32px; border-radius: 10px; box-shadow: 0 4px 10px rgba(37, 99, 235, 0.3); }
            .warning { background-color: #fffbeb; border-left: 4px solid #f59e0b; padding: 14px 16px; margin: 24px 0; border-radius: 4px; font-size: 13px; color: #92400e; }
            .fallback { font-size: 12px; color: #64748b; word-break: break-all; margin-top: 24px; }
            .footer { background-color: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <div class="logo">⚽ Fut<span>Manager</span></div>
            </div>
            <div class="content">
              <h1 class="greeting">Olá, %s!</h1>
              <p>Recebemos uma solicitação para redefinir a senha de acesso da sua conta no <strong>FutManager</strong>.</p>
              <p>Clique no botão abaixo para escolher uma nova senha segura:</p>
              <div class="btn-wrapper">
                <a href="%s" target="_blank" class="btn">Redefinir Minha Senha</a>
              </div>
              <div class="warning">
                ⏳ Este link expira em <strong>%d minutos</strong> e é de uso único. Após a redefinição, todas as sessões ativas serão encerradas.
              </div>
              <p class="fallback">
                Se o botão não funcionar, copie e cole o link no seu navegador:<br>
                <a href="%s" style="color: #2563eb;">%s</a>
              </p>
              <p style="font-size: 12px; color: #64748b; margin-top: 24px;">
                Se você não solicitou a redefinição de senha, ignore este e-mail. Sua senha atual permanecerá inalterada e sua conta protegida.
              </p>
            </div>
            <div class="footer">
              &copy; 2026 FutManager. Todos os direitos reservados.
            </div>
          </div>
        </body>
        </html>
        """.formatted(safeName, resetUrl, validityMinutes, resetUrl, resetUrl);
    }

    private String buildPasswordChangedTemplate(String name, String appUrl) {
        String safeName = (name != null && !name.isBlank()) ? name : "Usuário";
        return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Senha Alterada</title>
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 24px; color: #1e293b; }
            .container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border: 1px solid #e2e8f0; }
            .header { background: #0f172a; padding: 32px 24px; text-align: center; }
            .logo { font-size: 26px; font-weight: 800; color: #ffffff; margin: 0; }
            .logo span { color: #16a34a; }
            .content { padding: 32px 28px; line-height: 1.6; }
            .greeting { font-size: 18px; font-weight: 700; color: #0f172a; margin-top: 0; }
            .alert-box { background-color: #ecfdf5; border-left: 4px solid #10b981; padding: 14px 16px; margin: 24px 0; border-radius: 4px; font-size: 14px; color: #065f46; font-weight: 600; }
            .btn-wrapper { text-align: center; margin: 28px 0; }
            .btn { display: inline-block; background-color: #0f172a; color: #ffffff !important; text-decoration: none; font-weight: 700; font-size: 14px; padding: 12px 28px; border-radius: 10px; }
            .footer { background-color: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <div class="logo">⚽ Fut<span>Manager</span></div>
            </div>
            <div class="content">
              <h1 class="greeting">Olá, %s!</h1>
              <div class="alert-box">
                ✅ A senha da sua conta FutManager foi alterada com sucesso.
              </div>
              <p>Por medidas de segurança, todas as sessões anteriores foram revogadas. Você pode acessar sua conta utilizando a sua nova senha:</p>
              <div class="btn-wrapper">
                <a href="%s" target="_blank" class="btn">Acessar FutManager</a>
              </div>
              <p style="font-size: 12px; color: #e11d48; margin-top: 24px; font-weight: 600;">
                ⚠️ Se você NÃO realizou essa alteração, entre em contato imediatamente com a coordenação da sua escolinha ou com o administrador do sistema.
              </p>
            </div>
            <div class="footer">
              &copy; 2026 FutManager. Todos os direitos reservados.
            </div>
          </div>
        </body>
        </html>
        """.formatted(safeName, appUrl);
    }
}
