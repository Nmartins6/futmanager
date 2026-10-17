package com.futmanager.service;

import com.futmanager.config.JwtAuthenticationFilter;
import com.futmanager.config.JwtService;
import com.futmanager.controller.AuthController;
import com.futmanager.domain.entity.SecurityToken;
import com.futmanager.domain.entity.Tenant;
import com.futmanager.domain.entity.User;
import com.futmanager.domain.enums.Role;
import com.futmanager.domain.enums.TenantPlan;
import com.futmanager.domain.enums.TenantStatus;
import com.futmanager.domain.enums.TokenType;
import com.futmanager.domain.enums.UserStatus;
import com.futmanager.dto.AuthDtos.*;
import com.futmanager.dto.TenantDtos.TenantOnboardRequest;
import com.futmanager.exception.BusinessException;
import com.futmanager.repository.SecurityTokenRepository;
import com.futmanager.repository.TenantRepository;
import com.futmanager.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class EmailVerificationAndPasswordResetTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private SecurityTokenService securityTokenService;

    @Autowired
    private SecurityTokenRepository securityTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private EmailService emailService;

    private Tenant testTenant;
    private User testUser;

    @BeforeEach
    void setUp() {
        rateLimiterService.resetAll();
        securityTokenRepository.deleteAll();
        SecurityContextHolder.clearContext();

        testTenant = tenantRepository.findBySlug("escola-teste-seguranca")
                .orElseGet(() -> tenantRepository.save(Tenant.builder()
                        .name("Escola Teste Segurança")
                        .slug("escola-teste-seguranca")
                        .plan(TenantPlan.PRO)
                        .status(TenantStatus.ATIVO)
                        .maxStudents(100)
                        .build()));

        testUser = userRepository.findByEmail("usuario.seguranca@futmanager.com")
                .map(existing -> {
                    existing.setPasswordHash(passwordEncoder.encode("SenhaInicial123"));
                    existing.setStatus(UserStatus.ATIVO);
                    existing.setEmailVerified(false);
                    existing.setEmailVerifiedAt(null);
                    existing.setTokenVersion(1);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .tenant(testTenant)
                        .email("usuario.seguranca@futmanager.com")
                        .fullName("Usuário Segurança")
                        .role(Role.ADMINISTRADOR)
                        .status(UserStatus.ATIVO)
                        .passwordHash(passwordEncoder.encode("SenhaInicial123"))
                        .emailVerified(false)
                        .tokenVersion(1)
                        .build()));

        reset(emailService);
    }

    @Test
    @DisplayName("1. Deve enviar e-mail de confirmação ao cadastrar nova escolinha/usuário")
    void shouldSendEmailConfirmationOnOnboarding() {
        String uniqueEmail = "novo.admin." + System.currentTimeMillis() + "@escolinha.com";
        TenantOnboardRequest request = new TenantOnboardRequest(
                "Escolinha Nova Geração",
                null,
                "11.222.333/0001-44",
                "(11) 99999-0000",
                "Rua Teste, 10",
                TenantPlan.STARTER,
                "Admin Novo",
                uniqueEmail,
                "(11) 98888-0000",
                "SenhaForte123"
        );

        tenantService.onboardSchool(request);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1)).sendEmailVerification(eq(uniqueEmail), eq("Admin Novo"), tokenCaptor.capture());

        String rawToken = tokenCaptor.getValue();
        assertNotNull(rawToken);
        assertTrue(rawToken.length() >= 32, "Token deve possuir alta entropia");

        User createdUser = userRepository.findByEmail(uniqueEmail).orElseThrow();
        assertFalse(createdUser.isEmailVerified(), "Novo usuário deve iniciar com emailVerified = false");

        // O banco deve armazenar apenas o hash SHA-256 e nunca o token puro
        String expectedHash = SecurityTokenService.hashToken(rawToken);
        assertTrue(securityTokenRepository.findByTokenHashAndTokenType(expectedHash, TokenType.EMAIL_VERIFICATION).isPresent());
        assertTrue(securityTokenRepository.findByTokenHashAndTokenType(rawToken, TokenType.EMAIL_VERIFICATION).isEmpty());
    }

    @Test
    @DisplayName("2. Deve verificar e-mail com token válido e registrar data de confirmação")
    void shouldVerifyEmailWithValidToken() {
        String rawToken = securityTokenService.createEmailVerificationToken(testUser);

        var response = authController.verifyEmail(new VerifyEmailRequest(rawToken));
        assertEquals(200, response.getStatusCode().value());

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(updatedUser.isEmailVerified());
        assertNotNull(updatedUser.getEmailVerifiedAt());
    }

    @Test
    @DisplayName("3. Deve rejeitar token de verificação ou recuperação expirado")
    void shouldRejectExpiredToken() {
        String rawToken = securityTokenService.createPasswordResetToken(testUser);
        String hash = SecurityTokenService.hashToken(rawToken);

        SecurityToken tokenEntity = securityTokenRepository.findByTokenHashAndTokenType(hash, TokenType.PASSWORD_RESET).orElseThrow();
        tokenEntity.setExpiresAt(Instant.now().minus(24, ChronoUnit.HOURS));
        securityTokenRepository.saveAndFlush(tokenEntity);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authController.resetPassword(new ResetPasswordRequest(rawToken, "NovaSenha123"))
        );
        assertTrue(ex.getMessage().toLowerCase().contains("expirou"));
    }

    @Test
    @DisplayName("4. Deve impedir reutilização de token após o primeiro uso")
    void shouldRejectTokenReuse() {
        String rawToken = securityTokenService.createEmailVerificationToken(testUser);

        // Primeiro uso: sucesso
        authController.verifyEmail(new VerifyEmailRequest(rawToken));

        // Segundo uso com o mesmo token: deve falhar
        BusinessException ex = assertThrows(BusinessException.class, () ->
                authController.verifyEmail(new VerifyEmailRequest(rawToken))
        );
        assertTrue(ex.getMessage().toLowerCase().contains("utilizado"));
    }

    @Test
    @DisplayName("5. Deve permitir reenvio de confirmação e invalidar token anterior")
    void shouldResendVerificationAndInvalidatePreviousToken() {
        String firstToken = securityTokenService.createEmailVerificationToken(testUser);

        var response = authController.resendVerification(new ResendVerificationRequest(testUser.getEmail()));
        assertEquals(200, response.getStatusCode().value());

        ArgumentCaptor<String> newTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1)).sendEmailVerification(eq(testUser.getEmail()), eq(testUser.getFullName()), newTokenCaptor.capture());

        String secondToken = newTokenCaptor.getValue();
        assertNotEquals(firstToken, secondToken);

        // O primeiro token deve ter sido invalidado
        assertThrows(BusinessException.class, () ->
                authController.verifyEmail(new VerifyEmailRequest(firstToken))
        );

        // O segundo token deve funcionar normalmente
        authController.verifyEmail(new VerifyEmailRequest(secondToken));
        assertTrue(userRepository.findById(testUser.getId()).orElseThrow().isEmailVerified());
    }

    @Test
    @DisplayName("6 e 7. Solicitação de recuperação deve enviar e-mail para conta existente e manter resposta idêntica para e-mail inexistente")
    void shouldHandlePasswordRecoveryWithoutAccountEnumeration() {
        var existingRes = authController.forgotPassword(new PasswordRecoveryRequest(testUser.getEmail()));
        rateLimiterService.resetAll();
        var nonExistentRes = authController.forgotPassword(new PasswordRecoveryRequest("nao.existe.12345@futmanager.com"));

        assertEquals(200, existingRes.getStatusCode().value());
        assertEquals(200, nonExistentRes.getStatusCode().value());
        assertEquals(existingRes.getBody().message(), nonExistentRes.getBody().message(),
                "Mensagens devem ser idênticas para evitar enumeração de contas");

        verify(emailService, times(1)).sendPasswordReset(eq(testUser.getEmail()), eq(testUser.getFullName()), anyString());
        verify(emailService, never()).sendPasswordReset(eq("nao.existe.12345@futmanager.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("8. Deve redefinir senha com token válido, permitir login com nova senha e notificar por e-mail")
    void shouldResetPasswordSuccessfully() {
        authController.forgotPassword(new PasswordRecoveryRequest(testUser.getEmail()));

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordReset(eq(testUser.getEmail()), eq(testUser.getFullName()), tokenCaptor.capture());
        String resetToken = tokenCaptor.getValue();

        // Valida o token via endpoint de checagem prévia
        var validationRes = authController.validateResetToken(resetToken);
        assertTrue(validationRes.getBody().valid());

        // Redefine a senha
        var resetRes = authController.resetPassword(new ResetPasswordRequest(resetToken, "NovaSenhaSegura99!"));
        assertEquals(200, resetRes.getStatusCode().value());

        // Verifica envio de e-mail de confirmação de senha alterada
        verify(emailService, times(1)).sendPasswordChangedNotification(eq(testUser.getEmail()), eq(testUser.getFullName()));

        // Senha antiga deve ser rejeitada no login
        assertThrows(BusinessException.class, () ->
                authController.login(new LoginRequest(testUser.getEmail(), "SenhaInicial123"))
        );

        // Nova senha deve autenticar com sucesso
        var loginRes = authController.login(new LoginRequest(testUser.getEmail(), "NovaSenhaSegura99!"));
        assertEquals(200, loginRes.getStatusCode().value());
        assertNotNull(loginRes.getBody().token());
    }

    @Test
    @DisplayName("9. Deve validar regras da nova senha sem queimar o token em caso de erro de validação")
    void shouldValidateNewPasswordBeforeConsumingToken() {
        String rawToken = securityTokenService.createPasswordResetToken(testUser);

        // Tenta redefinir com senha muito curta (< 6 caracteres)
        assertThrows(BusinessException.class, () ->
                authController.resetPassword(new ResetPasswordRequest(rawToken, "123"))
        );

        // O token ainda deve estar válido para uma tentativa correta
        var res = authController.resetPassword(new ResetPasswordRequest(rawToken, "SenhaValida123"));
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    @DisplayName("10. Deve rejeitar token inválido ou adulterado")
    void shouldRejectInvalidToken() {
        assertThrows(BusinessException.class, () ->
                authController.validateResetToken("token-invalido-adulterado-1234567890")
        );
        assertThrows(BusinessException.class, () ->
                authController.resetPassword(new ResetPasswordRequest("token-invalido-adulterado-1234567890", "NovaSenha123"))
        );
    }

    @Test
    @DisplayName("11. Deve revogar sessões JWT anteriores após redefinição de senha")
    void shouldRevokePreviousSessionsAfterPasswordReset() throws Exception {
        // 1. Faz login e obtém JWT com tokenVersion = 1
        var loginBefore = authController.login(new LoginRequest(testUser.getEmail(), "SenhaInicial123")).getBody();
        assertNotNull(loginBefore);
        String oldJwt = loginBefore.token();

        // 2. Redefine a senha (incrementa tokenVersion para 2)
        String resetToken = securityTokenService.createPasswordResetToken(testUser);
        authController.resetPassword(new ResetPasswordRequest(resetToken, "NovaSenhaPosReset123"));

        // 3. Tenta usar o JWT antigo no filtro de autenticação
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("Authorization", "Bearer " + oldJwt);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus(), "JWT emitido antes da redefinição de senha deve ser rejeitado com 401");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("12. Deve aplicar rate limiting / cooldown em solicitações repetidas")
    void shouldEnforceRateLimitingAndCooldown() {
        // Primeira chamada: permitida
        authController.forgotPassword(new PasswordRecoveryRequest(testUser.getEmail()));

        // Segunda chamada imediata (< 60s de cooldown): bloqueada
        BusinessException cooldownEx = assertThrows(BusinessException.class, () ->
                authController.forgotPassword(new PasswordRecoveryRequest(testUser.getEmail()))
        );
        assertTrue(cooldownEx.getMessage().contains("60 segundos") || cooldownEx.getMessage().contains("Aguarde"));

        // Limite de tentativas no reset de senha para um mesmo token
        String rawToken = securityTokenService.createPasswordResetToken(testUser);
        for (int i = 0; i < 5; i++) {
            assertThrows(BusinessException.class, () ->
                    authController.resetPassword(new ResetPasswordRequest(rawToken, "curta"))
            );
        }
        BusinessException rateEx = assertThrows(BusinessException.class, () ->
                authController.resetPassword(new ResetPasswordRequest(rawToken, "SenhaValidaMasBloqueada123"))
        );
        assertTrue(rateEx.getMessage().toLowerCase().contains("tentativas"));
    }

    @Test
    @DisplayName("13. Deve garantir uso único mesmo sob requisições concorrentes simultâneas")
    void shouldPreventConcurrentTokenConsumption() throws InterruptedException {
        String rawToken = securityTokenService.createPasswordResetToken(testUser);

        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    authController.resetPassword(new ResetPasswordRequest(rawToken, "SenhaConcorrente" + idx));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> f : futures) {
            try {
                f.get(10, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }
        executor.shutdownNow();

        assertEquals(1, successCount.get(), "Apenas uma requisição concorrente pode conseguir consumir o token");
        assertEquals(threads - 1, failureCount.get(), "As demais requisições concorrentes devem ser rejeitadas");
    }
}
