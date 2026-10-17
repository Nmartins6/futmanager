package com.futmanager.repository;

import com.futmanager.domain.entity.SecurityToken;
import com.futmanager.domain.entity.User;
import com.futmanager.domain.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecurityTokenRepository extends JpaRepository<SecurityToken, UUID> {

    Optional<SecurityToken> findByTokenHashAndTokenType(String tokenHash, TokenType tokenType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SecurityToken t SET t.usedAt = :now WHERE t.user = :user AND t.tokenType = :tokenType AND t.usedAt IS NULL")
    void invalidateActiveTokensForUser(@Param("user") User user,
                                      @Param("tokenType") TokenType tokenType,
                                      @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SecurityToken t SET t.usedAt = :now WHERE t.id = :id AND t.usedAt IS NULL")
    int markTokenAsUsedAtomically(@Param("id") UUID id, @Param("now") Instant now);
}
