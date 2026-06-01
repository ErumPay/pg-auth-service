package com.erumpay.pg_auth_service.service;

import com.erumpay.pg_auth_service.client.KakaoClient;
import com.erumpay.pg_auth_service.dto.AuthStatusResponse;
import com.erumpay.pg_auth_service.dto.KakaoMerchantLoginRequest;
import com.erumpay.pg_auth_service.dto.KakaoMerchantLoginResponse;
import com.erumpay.pg_auth_service.dto.KakaoTokenResponse;
import com.erumpay.pg_auth_service.dto.KakaoUserResponse;
import com.erumpay.pg_auth_service.dto.LogoutRequest;
import com.erumpay.pg_auth_service.dto.MerchantSignupRequest;
import com.erumpay.pg_auth_service.dto.MerchantSignupResponse;
import com.erumpay.pg_auth_service.dto.MerchantTermsAgreeRequest;
import com.erumpay.pg_auth_service.dto.MerchantTermsAgreeResponse;
import com.erumpay.pg_auth_service.dto.TokenRefreshRequest;
import com.erumpay.pg_auth_service.dto.TokenRefreshResponse;
import com.erumpay.pg_auth_service.entity.MerchantAccountRole;
import com.erumpay.pg_auth_service.entity.MerchantAccountStatus;
import com.erumpay.pg_auth_service.entity.MerchantAuth;
import com.erumpay.pg_auth_service.entity.MerchantRefreshToken;
import com.erumpay.pg_auth_service.entity.MerchantTermsAgreement;
import com.erumpay.pg_auth_service.exception.AuthException;
import com.erumpay.pg_auth_service.repository.MerchantAuthRepository;
import com.erumpay.pg_auth_service.repository.MerchantRefreshTokenRepository;
import com.erumpay.pg_auth_service.repository.MerchantTermsAgreementRepository;
import com.erumpay.pg_auth_service.security.JwtProperties;
import com.erumpay.pg_auth_service.security.JwtRole;
import com.erumpay.pg_auth_service.security.JwtService;
import com.erumpay.pg_auth_service.security.JwtTokenType;
import com.erumpay.pg_auth_service.config.RedisConfig;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final KakaoClient kakaoClient;
	private final JwtService jwtService;
	private final JwtProperties jwtProperties;
	private final StringRedisTemplate redisTemplate;
	private final MerchantAuthRepository merchantAuthRepository;
	private final MerchantRefreshTokenRepository merchantRefreshTokenRepository;
	private final MerchantTermsAgreementRepository merchantTermsAgreementRepository;

	public AuthStatusResponse getStatus() {
		return new AuthStatusResponse("pg-auth-service", "UP");
	}

	@Transactional
	public KakaoMerchantLoginResponse loginMerchantWithKakao(KakaoMerchantLoginRequest request) {
		KakaoTokenResponse kakaoToken = kakaoClient.requestToken(request.code());
		if (kakaoToken == null || kakaoToken.accessToken() == null) {
			throw new AuthException("카카오 토큰 발급에 실패했습니다.");
		}

		KakaoUserResponse kakaoUser = kakaoClient.requestUserInfo(kakaoToken.accessToken());
		if (kakaoUser == null || kakaoUser.id() == null) {
			throw new AuthException("카카오 사용자 정보를 가져오지 못했습니다.");
		}

		String kakaoOauthId = String.valueOf(kakaoUser.id());
		return merchantAuthRepository.findByKakaoOauthId(kakaoOauthId)
			.map(this::loginExistingMerchant)
			.orElseGet(() -> registerDraftMerchant(kakaoOauthId, kakaoUser));
	}

	@Transactional
	public MerchantTermsAgreeResponse agreeMerchantTerms(MerchantTermsAgreeRequest request, String ipAddress) {
		if (!Boolean.TRUE.equals(request.serviceTermsAgreed())) {
			throw new AuthException("서비스 이용약관 동의는 필수입니다.");
		}
		if (!Boolean.TRUE.equals(request.privacyPolicyAgreed())) {
			throw new AuthException("개인정보 처리방침 동의는 필수입니다.");
		}

		MerchantAuth merchant = merchantAuthRepository.findById(request.accountId())
			.orElseThrow(() -> new AuthException("가맹점 계정을 찾을 수 없습니다."));

		MerchantTermsAgreement agreement = new MerchantTermsAgreement();
		agreement.setAccountId(merchant.getAccountId());
		agreement.setServiceTermsAgreed(request.serviceTermsAgreed());
		agreement.setPrivacyPolicyAgreed(request.privacyPolicyAgreed());
		agreement.setMarketingAgreed(Boolean.TRUE.equals(request.marketingAgreed()));
		agreement.setTermsVersion(request.termsVersion());
		agreement.setAgreedIp(ipAddress);

		MerchantTermsAgreement saved = merchantTermsAgreementRepository.save(agreement);
		return new MerchantTermsAgreeResponse(saved.getAgreementId(), saved.getAccountId(), true);
	}

	@Transactional
	public MerchantSignupResponse signupMerchant(MerchantSignupRequest request) {
		MerchantAuth merchant = merchantAuthRepository.findById(request.accountId())
			.orElseThrow(() -> new AuthException("가맹점 계정을 찾을 수 없습니다."));

		if (merchant.getStatus() != MerchantAccountStatus.DRAFT) {
			throw new AuthException("회원가입 추가정보를 입력할 수 없는 상태입니다.");
		}

		// TODO: merchant-service에 사업자명, 사업자등록번호, 대표자명 등의 추가 정보를 전달합니다.
		merchant.setStatus(MerchantAccountStatus.PENDING);

		return new MerchantSignupResponse(merchant.getAccountId(), merchant.getStatus());
	}

	@Transactional(readOnly = true)
	public TokenRefreshResponse refreshAccessToken(TokenRefreshRequest request) {
		String refreshToken = request.refreshToken();
		if (!jwtService.validateToken(refreshToken)
			|| !JwtTokenType.REFRESH.name().equals(jwtService.extractTokenType(refreshToken))) {
			throw new AuthException("유효하지 않은 Refresh Token입니다.");
		}

		Long accountId = jwtService.extractAccountId(refreshToken);
		String redisKey = RedisConfig.MERCHANT_REFRESH_KEY_PREFIX + accountId;
		String savedToken = redisTemplate.opsForValue().get(redisKey);
		if (!refreshToken.equals(savedToken)) {
			throw new AuthException("Redis에 저장된 Refresh Token과 일치하지 않습니다.");
		}

		String tokenHash = hashToken(refreshToken);
		merchantRefreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash)
			.orElseThrow(() -> new AuthException("폐기되었거나 존재하지 않는 Refresh Token입니다."));

		return new TokenRefreshResponse(jwtService.createAccessToken(accountId, JwtRole.MERCHANT));
	}

	@Transactional
	public void logoutMerchant(LogoutRequest request) {
		String refreshToken = request.refreshToken();
		if (jwtService.validateToken(refreshToken)) {
			Long accountId = jwtService.extractAccountId(refreshToken);
			redisTemplate.delete(RedisConfig.MERCHANT_REFRESH_KEY_PREFIX + accountId);

			String tokenHash = hashToken(refreshToken);
			merchantRefreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash)
				.ifPresent(token -> token.setIsRevoked(true));
		}

		if (request.accessToken() != null && !request.accessToken().isBlank()) {
			redisTemplate.opsForValue().set(
				RedisConfig.ACCESS_BLACKLIST_KEY_PREFIX + request.accessToken(),
				"logout",
				jwtProperties.accessTokenExpiration(),
				TimeUnit.MILLISECONDS
			);
		}
	}

	private KakaoMerchantLoginResponse loginExistingMerchant(MerchantAuth merchant) {
		merchant.setLastLoginAt(LocalDateTime.now());
		String accessToken = jwtService.createAccessToken(merchant.getAccountId(), JwtRole.MERCHANT);
		String refreshToken = jwtService.createRefreshToken(merchant.getAccountId(), JwtRole.MERCHANT);
		saveRefreshToken(merchant.getAccountId(), refreshToken);

		return new KakaoMerchantLoginResponse(
			false,
			merchant.getAccountId(),
			merchant.getStatus(),
			accessToken,
			refreshToken,
			null
		);
	}

	private KakaoMerchantLoginResponse registerDraftMerchant(String kakaoOauthId, KakaoUserResponse kakaoUser) {
		MerchantAuth merchant = new MerchantAuth();
		merchant.setKakaoOauthId(kakaoOauthId);
		merchant.setName(extractNickname(kakaoUser));
		merchant.setRole(MerchantAccountRole.OWNER);
		merchant.setStatus(MerchantAccountStatus.DRAFT);

		MerchantAuth saved = merchantAuthRepository.save(merchant);

		// 신규 가맹점은 아직 정식 로그인 상태가 아니므로 accessToken/refreshToken을 발급하지 않습니다.
		// 대신 약관 동의와 추가정보 입력 단계에서 본인을 식별할 수 있도록 SIGNUP 타입의 임시 JWT를 발급합니다.
		// 이 토큰 안에는 accountId가 들어있어서, 다음 API에서 request body로 accountId를 받을 필요가 없어집니다.
		String signupToken = jwtService.createSignupToken(saved.getAccountId());

		return new KakaoMerchantLoginResponse(
			true,
			saved.getAccountId(),
			saved.getStatus(),
			null,
			null,
			signupToken
		);
	}

	private void saveRefreshToken(Long accountId, String refreshToken) {
		redisTemplate.opsForValue().set(
			RedisConfig.MERCHANT_REFRESH_KEY_PREFIX + accountId,
			refreshToken,
			jwtProperties.refreshTokenExpiration(),
			TimeUnit.MILLISECONDS
		);

		MerchantRefreshToken token = new MerchantRefreshToken();
		token.setAccountId(accountId);
		token.setTokenHash(hashToken(refreshToken));
		token.setExpiresAt(toLocalDateTime(jwtProperties.refreshTokenExpiration()));
		token.setIsRevoked(false);
		merchantRefreshTokenRepository.save(token);
	}

	private LocalDateTime toLocalDateTime(long expirationMillis) {
		return LocalDateTime.ofInstant(
			Instant.now().plusMillis(expirationMillis),
			ZoneId.systemDefault()
		);
	}

	private String extractNickname(KakaoUserResponse kakaoUser) {
		if (kakaoUser.kakaoAccount() != null
			&& kakaoUser.kakaoAccount().profile() != null
			&& kakaoUser.kakaoAccount().profile().nickname() != null) {
			return kakaoUser.kakaoAccount().profile().nickname();
		}
		return "카카오 가맹점";
	}

	private String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new AuthException("토큰 해시 생성에 실패했습니다.");
		}
	}
}
