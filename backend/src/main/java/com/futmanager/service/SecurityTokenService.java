package com.futmanager.service;

import com.futmanager.domain.entity.SecurityToken;
import com.futmanager.domain.entity.User;
import com.futmanager.domain.enums.TokenType;
import com.futmanager.exception.BusinessException;
import com.futmanager.repository.SecurityTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityTokenService {

    private final SecurityTokenRepository securityTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${futmanager.security.password-reset-token-validity-minutes:15}")
    private int passwordResetValidityMinutes;

    @Value("${futmanager.security.email-verification-token-validity-hours:24}")
    private int emailVerificationValidityHours;

    @Transactional
    public String createPasswordResetToken(User user) {
        Instant now = Instant.now();
        securityTokenRepository.invalidateActiveTokensForUser(user, TokenType.PASSWORD_RESET, now);

        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = now.plus(passwordResetValidityMinutes, ChronoUnit.MINUTES);

        SecurityToken token = SecurityToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .tokenType(TokenType.PASSWORD_RESET)
                .expiresAt(expiresAt)
                .build();

        securityTokenRepository.save(token);
        log.info("Token de recuperação de senha gerado para usuário com ID: {} (expira em {} minutos)",
                user.getId(), passwordResetValidityMinutes);

        return rawToken;
    }

    @Transactional
    public String createEmailVerificationToken(User user) {
        Instant now = Instant.now();
        securityTokenRepository.invalidateActiveTokensForUser(user, TokenType.EMAIL_VERIFICATION, now);

        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = now.plus(emailVerificationValidityHours, ChronoUnit.HOURS);

        SecurityToken token = SecurityToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .tokenType(TokenType.EMAIL_VERIFICATION)
                .expiresAt(expiresAt)
                .build();

        securityTokenRepository.save(token);
        log.info("Token de verificação de e-mail gerado para usuário com ID: {} (expira em {} horas)",
                user.getId(), emailVerificationValidityHours);

        return rawToken;
    }

    @Transactional(readOnly = true)
    public SecurityToken validateToken(String rawToken, TokenType tokenType) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException("Token de segurança não fornecido.");
        }

        String hash = hashToken(rawToken);
        SecurityToken token = securityTokenRepository.findByTokenHashAndTokenType(hash, tokenType)
                .orElseThrow(() -> new BusinessException("Token inválido ou inexistente."));

        if (token.isUsed()) {
            throw new BusinessException("Este link já foi utilizado anteriormente. Por favor, solicite um novo link.");
        }

        if (token.isExpired()) {
            throw new BusinessException("Este link expirou. Por motivos de segurança, solicite um novo link.");
        }

        return token;
    }

    @Transactional
    public User consumeToken(String rawToken, TokenType tokenType) {
        SecurityToken token = validateToken(rawToken, tokenType);
        User user = token.getUser();
        // Force initialization of user proxy before clearAutomatically
        user.getId();
        user.getEmail();

        int updated = securityTokenRepository.markTokenAsUsedAtomically(token.getId(), Instant.now());
        if (updated == 0) {
            throw new BusinessException("Este link já foi utilizado anteriormente. Por favor, solicite um novo link.");
        }
        return user;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32]; // 256 bits de entropia criptográfica
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashToken(String rawToken) {
        if (rawToken == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível no ambiente", e);
        }
    }
}
