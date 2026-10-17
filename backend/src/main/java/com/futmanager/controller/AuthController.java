package com.futmanager.controller;

import com.futmanager.config.JwtService;
import com.futmanager.config.UserPrincipal;
import com.futmanager.domain.entity.SecurityToken;
import com.futmanager.domain.entity.User;
import com.futmanager.domain.enums.TokenType;
import com.futmanager.domain.enums.UserStatus;
import com.futmanager.dto.AuthDtos.*;
import com.futmanager.exception.BusinessException;
import com.futmanager.repository.UserRepository;
import com.futmanager.service.EmailService;
import com.futmanager.service.RateLimiterService;
import com.futmanager.service.SecurityTokenService;
import com.futmanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de autenticação, perfil, verificação de e-mail e recuperação de senha")
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserService userService;
    private final SecurityTokenService securityTokenService;
    private final EmailService emailService;
    private final RateLimiterService rateLimiterService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private static final String GENERIC_RECOVERY_MESSAGE =
            "Se o endereço informado estiver associado a uma conta, você receberá um e-mail com as instruções para redefinir sua senha.";

    private static final String GENERIC_VERIFICATION_RESEND_MESSAGE =
            "Se o endereço informado estiver pendente de confirmação, um novo link de verificação foi enviado para o seu e-mail.";

    @PostMapping("/login")
    @Operation(summary = "Login no sistema com validação de senha segura")
    @Transactional(readOnly = true)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().toLowerCase().trim();
        com.futmanager.util.ValidationUtils.validateEmail(email, "O e-mail");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Credenciais inválidas. E-mail ou senha incorretos."));

        if (user.getStatus() != UserStatus.ATIVO) {
            throw new BusinessException("Usuário inativado pelo administrador. Acesso não permitido.");
        }

        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("A senha de acesso é obrigatória.");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Credenciais inválidas. E-mail ou senha incorretos.");
        }

        UUID tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        String tenantName = user.getTenant() != null ? user.getTenant().getName() : null;
        String tenantSlug = user.getTenant() != null ? user.getTenant().getSlug() : null;
        String tenantPlan = user.getTenant() != null && user.getTenant().getPlan() != null ? user.getTenant().getPlan().name() : "STARTER";

        String token = jwtService.generateDevToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                tenantId,
                tenantSlug,
                user.getTokenVersion()
        );

        return ResponseEntity.ok(new LoginResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                tenantId,
                tenantName,
                tenantSlug,
                tenantPlan,
                user.isEmailVerified()
        ));
    }

    @GetMapping("/me")
    @Operation(summary = "Obter dados do usuário autenticado atual")
    public ResponseEntity<UserPrincipal> getCurrentUser() {
        UserPrincipal principal = userService.getCurrentUserPrincipal();
        if (principal == null) {
            throw new BusinessException("Usuário não autenticado.");
        }
        return ResponseEntity.ok(principal);
    }

    @PostMapping({"/forgot-password", "/recover-password"})
    @Operation(summary = "Solicitar link seguro de recuperação de senha por e-mail")
    @Transactional
    public ResponseEntity<PasswordRecoveryResponse> forgotPassword(@Valid @RequestBody PasswordRecoveryRequest request) {
        String email = request.email().toLowerCase().trim();
        com.futmanager.util.ValidationUtils.validateEmail(email, "O e-mail");

        // Rate limiting contra abuso e solicitações automatizadas excessivas
        rateLimiterService.checkCooldown(
                "forgot_cooldown:" + email,
                60,
                "Aguarde 60 segundos antes de solicitar um novo link de recuperação."
        );
        rateLimiterService.checkRateLimit(
                "forgot_window:" + email,
                5,
                15,
                "Limite de solicitações de recuperação excedido. Tente novamente em 15 minutos."
        );

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getStatus() == UserStatus.ATIVO) {
                String rawToken = securityTokenService.createPasswordResetToken(user);
                emailService.sendPasswordReset(user.getEmail(), user.getFullName(), rawToken);
                log.info("[AUDITORIA] Solicitação de recuperação de senha processada para usuário ID: {}", user.getId());
            } else {
                log.warn("[AUDITORIA] Solicitação de recuperação ignorada para conta inativa ID: {}", user.getId());
            }
        } else {
            log.info("[AUDITORIA] Solicitação de recuperação recebida para e-mail não cadastrado (proteção contra enumeração ativa)");
        }

        // Sempre retorna mensagem genérica para evitar enumeração de contas (OWASP)
        return ResponseEntity.ok(new PasswordRecoveryResponse(GENERIC_RECOVERY_MESSAGE, null));
    }

    @GetMapping("/validate-reset-token")
    @Operation(summary = "Validar token de redefinição de senha antes de exibir o formulário")
    @Transactional(readOnly = true)
    public ResponseEntity<TokenValidationResponse> validateResetToken(@RequestParam("token") String token) {
        SecurityToken securityToken = securityTokenService.validateToken(token, TokenType.PASSWORD_RESET);
        User user = securityToken.getUser();

        if (user.getStatus() != UserStatus.ATIVO) {
            throw new BusinessException("Esta conta está inativa. Entre em contato com a administração.");
        }

        return ResponseEntity.ok(new TokenValidationResponse(
                true,
                maskEmail(user.getEmail()),
                "Token de recuperação válido."
        ));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefinir senha utilizando token criptográfico de uso único")
    @Transactional
    public ResponseEntity<PasswordRecoveryResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String rawToken = request.token();
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException("Token de recuperação obrigatório. Utilize o link enviado para o seu e-mail.");
        }

        String tokenKey = SecurityTokenService.hashToken(rawToken);
        rateLimiterService.checkRateLimit(
                "reset_attempt:" + tokenKey,
                5,
                15,
                "Número excessivo de tentativas. Por segurança, solicite um novo link de recuperação."
        );

        if (request.newPassword() == null || request.newPassword().trim().length() < 6) {
            throw new BusinessException("A nova senha deve ter no mínimo 6 caracteres.");
        }

        // Consome e invalida o token de forma transacional (uso único)
        User user = securityTokenService.consumeToken(rawToken, TokenType.PASSWORD_RESET);

        if (user.getStatus() != UserStatus.ATIVO) {
            throw new BusinessException("Esta conta está desativada. Entre em contato com a coordenação.");
        }

        // Atualiza a senha com PasswordEncoder (BCrypt) e incrementa tokenVersion para revogar sessões anteriores
        user.setPasswordHash(passwordEncoder.encode(request.newPassword().trim()));
        int nextVersion = (user.getTokenVersion() != null ? user.getTokenVersion() : 1) + 1;
        user.setTokenVersion(nextVersion);
        userRepository.save(user);

        log.info("[AUDITORIA] Senha redefinida com sucesso e sessões anteriores revogadas (tokenVersion={}) para usuário ID: {}",
                nextVersion, user.getId());

        // Notifica o usuário por e-mail sobre a alteração da senha
        emailService.sendPasswordChangedNotification(user.getEmail(), user.getFullName());

        return ResponseEntity.ok(new PasswordRecoveryResponse(
                "Senha redefinida com sucesso! Você já pode entrar com sua nova senha.",
                null
        ));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Confirmar endereço de e-mail com token de verificação")
    @Transactional
    public ResponseEntity<GenericMessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return processEmailVerification(request.token());
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Confirmar endereço de e-mail via link (GET)")
    @Transactional
    public ResponseEntity<GenericMessageResponse> verifyEmailGet(@RequestParam("token") String token) {
        return processEmailVerification(token);
    }

    private ResponseEntity<GenericMessageResponse> processEmailVerification(String rawToken) {
        User user = securityTokenService.consumeToken(rawToken, TokenType.EMAIL_VERIFICATION);

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(Instant.now());
            userRepository.save(user);
            log.info("[AUDITORIA] E-mail confirmado com sucesso para usuário ID: {}", user.getId());
        }

        return ResponseEntity.ok(new GenericMessageResponse("Endereço de e-mail confirmado com sucesso!"));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Reenviar link de confirmação de e-mail com proteção contra abuso")
    @Transactional
    public ResponseEntity<GenericMessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        String email = request.email().toLowerCase().trim();
        com.futmanager.util.ValidationUtils.validateEmail(email, "O e-mail");

        rateLimiterService.checkCooldown(
                "verify_cooldown:" + email,
                60,
                "Por favor, aguarde 60 segundos antes de solicitar um novo e-mail de confirmação."
        );
        rateLimiterService.checkRateLimit(
                "verify_window:" + email,
                5,
                15,
                "Muitas solicitações de reenvio. Aguarde alguns minutos antes de tentar novamente."
        );

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getStatus() == UserStatus.ATIVO && !user.isEmailVerified()) {
                String rawToken = securityTokenService.createEmailVerificationToken(user);
                emailService.sendEmailVerification(user.getEmail(), user.getFullName(), rawToken);
                log.info("[AUDITORIA] Link de confirmação de e-mail reenviado para usuário ID: {}", user.getId());
            } else if (user.isEmailVerified()) {
                log.info("[AUDITORIA] Solicitação de reenvio de confirmação ignorada (e-mail já verificado) para usuário ID: {}", user.getId());
            }
        }

        return ResponseEntity.ok(new GenericMessageResponse(GENERIC_VERIFICATION_RESEND_MESSAGE));
    }

    private String maskEmail(String email) {
        if (email == null) return "";
        int at = email.indexOf('@');
        if (at > 2) {
            return email.substring(0, 2) + "***" + email.substring(at);
        }
        return "***";
    }
}
