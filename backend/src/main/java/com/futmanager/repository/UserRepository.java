package com.futmanager.repository;

import com.futmanager.domain.entity.User;
import com.futmanager.domain.enums.Role;
import com.futmanager.domain.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByTenantIdAndRoleAndStatus(UUID tenantId, Role role, UserStatus status);

    @Query("SELECT u FROM User u WHERE " +
           "u.tenant.id = :tenantId AND " +
           "(:query IS NULL OR :query = '' OR LOWER(u.fullName) LIKE :query OR LOWER(u.email) LIKE :query) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.status = :status)")
    Page<User> searchUsers(@Param("tenantId") UUID tenantId,
                           @Param("query") String query,
                           @Param("role") Role role,
                           @Param("status") UserStatus status,
                           Pageable pageable);

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
}
