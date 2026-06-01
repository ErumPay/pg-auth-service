package com.erumpay.pg_auth_service.repository;

import com.erumpay.pg_auth_service.entity.MerchantRefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRefreshTokenRepository extends JpaRepository<MerchantRefreshToken, Long> {

	Optional<MerchantRefreshToken> findByTokenHashAndIsRevokedFalse(String tokenHash);

	List<MerchantRefreshToken> findAllByAccountIdAndIsRevokedFalse(Long accountId);
}
