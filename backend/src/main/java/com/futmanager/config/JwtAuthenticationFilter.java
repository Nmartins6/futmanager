package com.futmanager.config;

import com.futmanager.domain.entity.User;
import com.futmanager.domain.enums.UserStatus;
import com.futmanager.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String bearerToken = extractTokenFromRequest(request);

            if (StringUtils.hasText(bearerToken)) {
                Claims claims = jwtService.validateAndExtractClaims(bearerToken);
                if (claims != null) {
                    String email = claims.get("email", String.class);
                    String sub = claims.getSubject();

                    Optional<User> userOpt = Optional.empty();

                    if (StringUtils.hasText(email)) {
                        userOpt = userRepository.findByEmail(email.toLowerCase().trim());
                    }
                    if (userOpt.isEmpty() && StringUtils.hasText(sub)) {
                        try {
                            UUID userId = UUID.fromString(sub);
                            userOpt = userRepository.findById(userId);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                    if (userOpt.isPresent()) {
                        User user = userOpt.get();

                        if (user.getStatus() != UserStatus.ATIVO) {
                            log.warn("Tentativa de acesso por usuário inativo: {}", user.getEmail());
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"Acesso negado: Usuário inativo no sistema.\"}");
                            return;
                        }

                        // Validação de revogação de sessão (ex: senha alterada)
                        Object tvClaim = claims.get("token_version");
                        if (tvClaim instanceof Number tvNumber) {
                            int tokenVersion = tvNumber.intValue();
                            int currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 1;
                            if (tokenVersion != currentVersion) {
                                log.warn("Sessão revogada para usuário {}: versão do token ({}) incompatível com a versão atual ({})",
                                        user.getEmail(), tokenVersion, currentVersion);
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"error\":\"Sessão expirada ou senha alterada. Por favor, faça login novamente.\"}");
                                return;
                            }
                        }

                        // Vincular o tenant do usuário ao contexto da requisição
                        if (user.getTenant() != null) {
                            TenantContext.setTenantId(user.getTenant().getId());
                        }

                        UserPrincipal principal = UserPrincipal.fromUser(user);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        principal,
                                        null,
                                        principal.getAuthorities()
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
