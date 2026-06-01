package com.erumpay.pg_auth_service.repository;

import com.erumpay.pg_auth_service.entity.AdminRefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, Long> {

	Optional<AdminRefreshToken> findByTokenHashAndIsRevokedFalse(String tokenHash);
}
