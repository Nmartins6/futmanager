package com.futmanager.controller;

import com.futmanager.config.JwtService;
import com.futmanager.config.UserPrincipal;
import com.futmanager.domain.entity.User;
import com.futmanager.domain.enums.UserStatus;
import com.futmanager.dto.AuthDtos.*;
import com.futmanager.exception.BusinessException;
import com.futmanager.repository.UserRepository;
import com.futmanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de autenticação, perfil e recuperação de senha")
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserService userService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private static final java.util.concurrent.ConcurrentHashMap<String, PasswordResetToken> resetTokens = new java.util.concurrent.ConcurrentHashMap<>();

    private record PasswordResetToken(String code, java.time.Instant expiresAt) {}

    @PostMapping("/login")
    @Operation(summary = "Login no sistema com validação de senha segura")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().toLowerCase().trim();
        com.futmanager.util.ValidationUtils.validateEmail(email, "O e-mail");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Credenciais inválidas. E-mail ou senha incorretos."));

        if (user.getStatus() != UserStatus.ATIVO) {
            throw new BusinessException("Usuário inativado pelo administrador. Acesso não permitido.");
        }

        // Validação estrita de senha com hash BCrypt
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

        String token = jwtService.generateDevToken(user.getId(), user.getEmail(), user.getRole().name(), tenantId, tenantSlug);

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
                tenantPlan
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

    @PostMapping("/recover-password")
    @Operation(summary = "Solicitar código de recuperação de senha")
    public ResponseEntity<PasswordRecoveryResponse> recoverPassword(@Valid @RequestBody PasswordRecoveryRequest request) {
        String email = request.email().toLowerCase().trim();
        com.futmanager.util.ValidationUtils.validateEmail(email, "O e-mail");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Nenhum usuário cadastrado encontrado com o e-mail: " + email));

        if (user.getStatus() != UserStatus.ATIVO) {
            throw new BusinessException("Esta conta está desativada. Entre em contato com a coordenação.");
        }

        // Gera código numérico seguro de 6 dígitos
        String code = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
        resetTokens.put(email, new PasswordResetToken(code, java.time.Instant.now().plus(15, java.time.temporal.ChronoUnit.MINUTES)));

        log.info("Código de recuperação gerado para {}: {}", email, code);

        return ResponseEntity.ok(new PasswordRecoveryResponse(
                "Código de verificação de 6 dígitos gerado com sucesso! Válido por 15 minutos.",
                code
        ));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefinir senha com código de verificação")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<PasswordRecoveryResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String email = request.email().toLowerCase().trim();
        com.futmanager.util.ValidationUtils.validateEmail(email, "O e-mail");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado com o e-mail: " + email));

        PasswordResetToken token = resetTokens.get(email);
        if (token == null || token.expiresAt().isBefore(java.time.Instant.now())) {
            throw new BusinessException("Código de recuperação inválido ou expirado. Solicite um novo código.");
        }

        if (request.code() == null || !token.code().equals(request.code().trim())) {
            throw new BusinessException("Código de verificação incorreto. Digite os 6 dígitos informados.");
        }

        if (request.newPassword() == null || request.newPassword().trim().length() < 6) {
            throw new BusinessException("A nova senha deve ter no mínimo 6 caracteres.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword().trim()));
        userRepository.save(user);

        // Invalida o token após uso
        resetTokens.remove(email);

        log.info("Senha redefinida com sucesso para o usuário: {}", email);
        return ResponseEntity.ok(new PasswordRecoveryResponse(
                "Senha redefinida com sucesso! Você já pode entrar com sua nova senha.",
                null
        ));
    }
}
