package com.futmanager.config;

import com.futmanager.domain.entity.User;
import com.futmanager.domain.enums.Role;
import com.futmanager.domain.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String fullName;
    private final Role role;
    private final UserStatus status;
    private final UUID tenantId;
    private final String tenantName;
    private final String tenantSlug;
    private final boolean emailVerified;
    private final Integer tokenVersion;

    public static UserPrincipal fromUser(User user) {
        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .tenantId(user.getTenant() != null ? user.getTenant().getId() : null)
                .tenantName(user.getTenant() != null ? user.getTenant().getName() : null)
                .tenantSlug(user.getTenant() != null ? user.getTenant().getSlug() : null)
                .emailVerified(user.isEmailVerified())
                .tokenVersion(user.getTokenVersion() != null ? user.getTokenVersion() : 1)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status == UserStatus.ATIVO;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ATIVO;
    }

    public boolean isSuperAdmin() {
        return role == Role.SUPER_ADMIN;
    }

    public boolean isAdmin() {
        return role == Role.ADMINISTRADOR || role == Role.SUPER_ADMIN;
    }

    public boolean isProfessor() {
        return role == Role.PROFESSOR;
    }
}
