package com.futmanager.service;

import com.futmanager.config.UserPrincipal;
import com.futmanager.domain.entity.User;
import com.futmanager.domain.enums.Role;
import com.futmanager.domain.enums.UserStatus;
import com.futmanager.dto.AuthDtos.InviteUserRequest;
import com.futmanager.dto.UserDtos.UserResponse;
import com.futmanager.dto.UserDtos.UserUpdateRequest;
import com.futmanager.exception.BusinessException;
import com.futmanager.exception.ResourceNotFoundException;
import com.futmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(String query, Role role, UserStatus status, Pageable pageable) {
        User currentUser = getCurrentUserEntity();
        UUID tenantId = currentUser.getTenant().getId();
        String search = (query != null && !query.isBlank()) ? "%" + query.trim().toLowerCase() + "%" : null;
        return userRepository.searchUsers(tenantId, search, role, status, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listActiveTeachers() {
        User currentUser = getCurrentUserEntity();
        return userRepository.findByTenantIdAndRoleAndStatus(currentUser.getTenant().getId(), Role.PROFESSOR, UserStatus.ATIVO)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User currentUser = getCurrentUserEntity();
        User user = userRepository.findByIdAndTenantId(id, currentUser.getTenant().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
        return toResponse(user);
    }

    @Transactional
    public UserResponse inviteUser(InviteUserRequest request) {
        User currentUser = getCurrentUserEntity();
        String normalizedEmail = request.email().toLowerCase().trim();
        com.futmanager.util.ValidationUtils.validateEmail(normalizedEmail, "O e-mail do usuário");
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("Já existe um usuário cadastrado com este e-mail: " + normalizedEmail);
        }

        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().trim().length() < 6) {
                throw new BusinessException("A senha inicial deve ter no mínimo 6 caracteres.");
            }
        }

        String rawPassword = (request.password() != null && !request.password().isBlank())
                ? request.password().trim()
                : "admin123";

        User user = User.builder()
                .tenant(currentUser.getTenant())
                .email(normalizedEmail)
                .fullName(request.fullName().trim())
                .role(request.role())
                .status(UserStatus.ATIVO)
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();

        user = userRepository.save(user);
        log.info("Novo usuário provisionado para escolinha {}: {} ({})",
                currentUser.getTenant().getName(), user.getEmail(), user.getRole());
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {
        User currentUser = getCurrentUserEntity();
        User user = userRepository.findByIdAndTenantId(id, currentUser.getTenant().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));

        UserPrincipal currentPrincipal = getCurrentUserPrincipal();
        if (currentPrincipal != null && currentPrincipal.getId().equals(id) && request.status() == UserStatus.INATIVO) {
            throw new BusinessException("Você não pode desativar seu próprio usuário administrador.");
        }

        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setStatus(request.status());
        user.setPhone(request.phone());

        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public UserResponse toggleUserStatus(UUID id) {
        User currentUser = getCurrentUserEntity();
        User user = userRepository.findByIdAndTenantId(id, currentUser.getTenant().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));

        UserPrincipal currentPrincipal = getCurrentUserPrincipal();
        if (currentPrincipal != null && currentPrincipal.getId().equals(id) && user.getStatus() == UserStatus.ATIVO) {
            throw new BusinessException("Você não pode desativar seu próprio usuário administrador.");
        }

        user.setStatus(user.getStatus() == UserStatus.ATIVO ? UserStatus.INATIVO : UserStatus.ATIVO);
        user = userRepository.save(user);
        log.info("Situação do usuário {} alterada para: {}", user.getEmail(), user.getStatus());
        return toResponse(user);
    }

    public UserPrincipal getCurrentUserPrincipal() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        return null;
    }

    public User getCurrentUserEntity() {
        UserPrincipal principal = getCurrentUserPrincipal();
        if (principal == null) {
            throw new BusinessException("Nenhum usuário autenticado encontrado na sessão.");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado no banco de dados."));
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.getPhone(),
                user.getCreatedAt()
        );
    }
}
