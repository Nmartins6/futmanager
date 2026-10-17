package com.futmanager.service;

public interface EmailService {

    /**
     * Envia e-mail com link para confirmação de cadastro e verificação de e-mail.
     */
    void sendEmailVerification(String toEmail, String recipientName, String rawToken);

    /**
     * Envia e-mail com link para redefinição de senha segura.
     */
    void sendPasswordReset(String toEmail, String recipientName, String rawToken);

    /**
     * Envia notificação de segurança informando que a senha da conta foi alterada.
     */
    void sendPasswordChangedNotification(String toEmail, String recipientName);
}
