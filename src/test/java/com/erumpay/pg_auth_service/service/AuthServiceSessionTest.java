package com.erumpay.pg_auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.erumpay.pg_auth_service.client.KakaoClient;
import com.erumpay.pg_auth_service.client.MerchantServiceClient;
import com.erumpay.pg_auth_service.config.InternalApiProperties;
import com.erumpay.pg_auth_service.dto.KakaoMerchantLoginRequest;
import com.erumpay.pg_auth_service.dto.KakaoMerchantLoginResponse;
import com.erumpay.pg_auth_service.dto.KakaoTokenResponse;
import com.erumpay.pg_auth_service.dto.KakaoUserResponse;
import com.erumpay.pg_auth_service.dto.LogoutRequest;
import com.erumpay.pg_auth_service.dto.TokenRefreshRequest;
import com.erumpay.pg_auth_service.dto.TokenRefreshResponse;
import com.erumpay.pg_auth_service.entity.MerchantAccountRole;
import com.erumpay.pg_auth_service.entity.MerchantAccountStatus;
import com.erumpay.pg_auth_service.entity.MerchantAuth;
import com.erumpay.pg_auth_service.entity.MerchantRefreshToken;
import com.erumpay.pg_auth_service.repository.MerchantAuthRepository;
import com.erumpay.pg_auth_service.repository.MerchantRefreshTokenRepository;
import com.erumpay.pg_auth_service.repository.MerchantTermsAgreementRepository;
import com.erumpay.pg_auth_service.security.JwtProperties;
import com.erumpay.pg_auth_service.security.JwtRole;
import com.erumpay.pg_auth_service.security.JwtService;
import com.erumpay.pg_auth_service.security.JwtTokenType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AuthServiceSessionTest {

	@Mock private KakaoClient kakaoClient;
	@Mock private MerchantServiceClient merchantServiceClient;
	@Mock private JwtService jwtService;
	@Mock private StringRedisTemplate redisTemplate;
	@Mock private ValueOperations<String, String> valueOperations;
	@Mock private MerchantAuthRepository merchantAuthRepository;
	@Mock private MerchantRefreshTokenRepository merchantRefreshTokenRepository;
	@Mock private MerchantTermsAgreementRepository merchantTermsAgreementRepository;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		authService = new AuthService(
			kakaoClient,
			merchantServiceClient,
			jwtService,
			new JwtProperties("test-secret", 1_800_000, 1_209_600_000, 86_400_000, 1_800_000),
			redisTemplate,
			merchantAuthRepository,
			merchantRefreshTokenRepository,
			merchantTermsAgreementRepository,
			new InternalApiProperties("test-internal-key")
		);
	}

	@Test
	void activeMerchantLoginIssuesAccessAndRefreshTokens() {
		MerchantAuth merchant = activeMerchant();
		when(kakaoClient.requestToken("code"))
			.thenReturn(new KakaoTokenResponse("kakao-access", "kakao-refresh", "bearer", 3600));
		when(kakaoClient.requestUserInfo("kakao-access"))
			.thenReturn(new KakaoUserResponse(123L, null));
		when(merchantAuthRepository.findByKakaoOauthId("123")).thenReturn(Optional.of(merchant));
		when(jwtService.createMerchantAccessToken(1L, 10L)).thenReturn("access-token");
		when(jwtService.createRefreshToken(1L, JwtRole.MERCHANT)).thenReturn("refresh-token");

		KakaoMerchantLoginResponse response =
			authService.loginMerchantWithKakao(new KakaoMerchantLoginRequest("code"));

		assertThat(response.status()).isEqualTo(MerchantAccountStatus.ACTIVE);
		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isEqualTo("refresh-token");
		verify(valueOperations).set(
			"refresh:merchant:1",
			"refresh-token",
			1_209_600_000,
			TimeUnit.MILLISECONDS
		);
		verify(merchantRefreshTokenRepository).save(any(MerchantRefreshToken.class));
	}

	@Test
	void refreshTokenIssuesNewAccessToken() {
		MerchantRefreshToken savedToken = new MerchantRefreshToken();
		savedToken.setExpiresAt(LocalDateTime.now().plusDays(10));
		savedToken.setIsRevoked(false);
		when(jwtService.validateToken("refresh-token")).thenReturn(true);
		when(jwtService.extractTokenType("refresh-token")).thenReturn(JwtTokenType.REFRESH.name());
		when(jwtService.extractRole("refresh-token")).thenReturn(JwtRole.MERCHANT.name());
		when(jwtService.extractAccountId("refresh-token")).thenReturn(1L);
		when(valueOperations.get("refresh:merchant:1")).thenReturn("refresh-token");
		when(merchantRefreshTokenRepository.findByTokenHashAndIsRevokedFalse(anyString()))
			.thenReturn(Optional.of(savedToken));
		when(merchantAuthRepository.findById(1L)).thenReturn(Optional.of(activeMerchant()));
		when(jwtService.createMerchantAccessToken(1L, 10L)).thenReturn("new-access-token");

		TokenRefreshResponse response = authService.refreshAccessToken(
			null,
			new TokenRefreshRequest("refresh-token")
		);

		assertThat(response.accessToken()).isEqualTo("new-access-token");
		assertThat(response.refreshToken()).isEqualTo("refresh-token");
	}

	@Test
	void logoutRevokesRefreshTokenAndBlacklistsAccessToken() {
		MerchantRefreshToken savedToken = new MerchantRefreshToken();
		savedToken.setIsRevoked(false);
		when(jwtService.validateToken("refresh-token")).thenReturn(true);
		when(jwtService.extractTokenType("refresh-token")).thenReturn(JwtTokenType.REFRESH.name());
		when(jwtService.extractRole("refresh-token")).thenReturn(JwtRole.MERCHANT.name());
		when(jwtService.extractAccountId("refresh-token")).thenReturn(1L);
		when(merchantRefreshTokenRepository.findByTokenHashAndIsRevokedFalse(anyString()))
			.thenReturn(Optional.of(savedToken));

		authService.logoutMerchant(
			null,
			new LogoutRequest("access-token", "refresh-token")
		);

		assertThat(savedToken.getIsRevoked()).isTrue();
		verify(redisTemplate).delete("refresh:merchant:1");
		verify(valueOperations).set(
			"blacklist:access:access-token",
			"merchant_logout",
			1_800_000,
			TimeUnit.MILLISECONDS
		);
	}

	private MerchantAuth activeMerchant() {
		MerchantAuth merchant = new MerchantAuth();
		merchant.setAccountId(1L);
		merchant.setMerchantId(10L);
		merchant.setKakaoOauthId("123");
		merchant.setName("테스트 가맹점");
		merchant.setRole(MerchantAccountRole.OWNER);
		merchant.setStatus(MerchantAccountStatus.ACTIVE);
		return merchant;
	}
}
