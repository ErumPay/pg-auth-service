package com.erumpay.pg_auth_service.repository;

import com.erumpay.pg_auth_service.entity.MerchantAuth;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantAuthRepository extends JpaRepository<MerchantAuth, Long> {

	Optional<MerchantAuth> findByKakaoOauthId(String kakaoOauthId);

	Optional<MerchantAuth> findByMerchantId(Long merchantId);

	boolean existsByKakaoOauthId(String kakaoOauthId);
}
