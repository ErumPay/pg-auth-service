package com.erumpay.pg_auth_service.service;

import com.erumpay.pg_auth_service.client.KakaoClient;
import com.erumpay.pg_auth_service.client.MerchantServiceClient;
import com.erumpay.pg_auth_service.config.RedisConfig;
import com.erumpay.pg_auth_service.dto.AuthStatusResponse;
import com.erumpay.pg_auth_service.dto.KakaoMerchantLoginRequest;
import com.erumpay.pg_auth_service.dto.KakaoMerchantLoginResponse;
import com.erumpay.pg_auth_service.dto.KakaoTokenResponse;
import com.erumpay.pg_auth_service.dto.KakaoUserResponse;
import com.erumpay.pg_auth_service.dto.LogoutRequest;
import com.erumpay.pg_auth_service.dto.MerchantCreateRequest;
import com.erumpay.pg_auth_service.dto.MerchantCreateResponse;
import com.erumpay.pg_auth_service.dto.MerchantSignupRequest;
import com.erumpay.pg_auth_service.dto.MerchantSignupResponse;
import com.erumpay.pg_auth_service.dto.MerchantStatusUpdateRequest;
import com.erumpay.pg_auth_service.dto.MerchantStatusUpdateResponse;
import com.erumpay.pg_auth_service.dto.MerchantTermsAgreeRequest;
import com.erumpay.pg_auth_service.dto.MerchantTermsAgreeResponse;
import com.erumpay.pg_auth_service.dto.MerchantTokenRevokeResponse;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String DEFAULT_REVIEW_STATUS = "WAITING";
	private static final long REFRESH_ROTATION_THRESHOLD_MILLIS = TimeUnit.DAYS.toMillis(3);

	private final KakaoClient kakaoClient;
	private final MerchantServiceClient merchantServiceClient;
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
		if (request.code() == null || request.code().isBlank()) {
			throw new AuthException("카카오 인가 코드가 필요합니다.");
		}

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
	public MerchantTermsAgreeResponse agreeMerchantTerms(
		String authorization,
		MerchantTermsAgreeRequest request,
		String ipAddress
	) {
		if (!Boolean.TRUE.equals(request.serviceTermsAgreed())) {
			throw new AuthException("서비스 이용약관 동의는 필수입니다.");
		}
		if (!Boolean.TRUE.equals(request.privacyPolicyAgreed())) {
			throw new AuthException("개인정보 처리방침 동의는 필수입니다.");
		}
		requireText(request.termsVersion(), "약관 버전은 필수입니다.");

		Long accountId = extractSignupAccountId(authorization);
		MerchantAuth merchant = findSignupMerchant(accountId);

		if (merchantTermsAgreementRepository.existsByAccountId(merchant.getAccountId())) {
			throw new AuthException("이미 약관 동의가 완료된 가맹점 계정입니다.");
		}

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
	public MerchantSignupResponse signupMerchant(String authorization, MerchantSignupRequest request) {
		validateSignupRequest(request);

		Long accountId = extractSignupAccountId(authorization);
		MerchantAuth merchant = findSignupMerchant(accountId);
		if (!merchantTermsAgreementRepository.existsByAccountId(accountId)) {
			throw new AuthException("약관 동의 후 회원가입을 진행할 수 있습니다.");
		}

		// merchant-service 내부 가맹점 생성 API는 Idempotency-Key가 필수입니다.
		String idempotencyKey = "merchant-signup-" + accountId;
		MerchantCreateResponse merchantResponse = merchantServiceClient.createMerchant(
			idempotencyKey,
			toMerchantCreateRequest(request)
		);

		if (merchantResponse == null || merchantResponse.merchantId() == null) {
			throw new AuthException("merchant-service 가맹점 생성 응답이 올바르지 않습니다.");
		}

		merchant.setMerchantId(merchantResponse.merchantId());
		merchant.setName(request.merchantName());
		merchant.setStatus(MerchantAccountStatus.PENDING);

		String reviewStatus = merchantResponse.reviewStatus() == null
			? DEFAULT_REVIEW_STATUS
			: merchantResponse.reviewStatus();
		return new MerchantSignupResponse(merchant.getMerchantId(), merchant.getStatus(), reviewStatus);
	}

	@Transactional
	public TokenRefreshResponse refreshAccessToken(String authorization, TokenRefreshRequest request) {
		String refreshToken = resolveRefreshToken(authorization, request == null ? null : request.refreshToken());
		if (!jwtService.validateToken(refreshToken)
			|| !JwtTokenType.REFRESH.name().equals(jwtService.extractTokenType(refreshToken))
			|| !JwtRole.MERCHANT.name().equals(jwtService.extractRole(refreshToken))) {
			throw new AuthException("유효하지 않은 가맹점 Refresh Token입니다.");
		}

		Long accountId = jwtService.extractAccountId(refreshToken);
		String redisKey = RedisConfig.MERCHANT_REFRESH_KEY_PREFIX + accountId;
		String savedToken = redisTemplate.opsForValue().get(redisKey);
		if (!refreshToken.equals(savedToken)) {
			throw new AuthException("저장된 Refresh Token과 일치하지 않습니다.");
		}

		MerchantRefreshToken savedRefreshToken = merchantRefreshTokenRepository
			.findByTokenHashAndIsRevokedFalse(hashToken(refreshToken))
			.orElseThrow(() -> new AuthException("폐기되었거나 존재하지 않는 Refresh Token입니다."));

		String newAccessToken = jwtService.createAccessToken(accountId, JwtRole.MERCHANT);
		if (shouldRotateRefreshToken(savedRefreshToken)) {
			savedRefreshToken.setIsRevoked(true);
			String newRefreshToken = jwtService.createRefreshToken(accountId, JwtRole.MERCHANT);
			saveRefreshToken(accountId, newRefreshToken);
			return new TokenRefreshResponse(newAccessToken, newRefreshToken);
		}

		return new TokenRefreshResponse(newAccessToken, refreshToken);
	}

	@Transactional
	public void logoutMerchant(String authorization, LogoutRequest request) {
		String refreshToken = resolveRefreshToken(authorization, request == null ? null : request.refreshToken());
		if (!jwtService.validateToken(refreshToken)
			|| !JwtTokenType.REFRESH.name().equals(jwtService.extractTokenType(refreshToken))
			|| !JwtRole.MERCHANT.name().equals(jwtService.extractRole(refreshToken))) {
			throw new AuthException("유효하지 않은 가맹점 Refresh Token입니다.");
		}

		Long accountId = jwtService.extractAccountId(refreshToken);
		redisTemplate.delete(RedisConfig.MERCHANT_REFRESH_KEY_PREFIX + accountId);
		merchantRefreshTokenRepository.findByTokenHashAndIsRevokedFalse(hashToken(refreshToken))
			.ifPresent(token -> token.setIsRevoked(true));

		if (request != null && request.accessToken() != null && !request.accessToken().isBlank()) {
			blacklistAccessToken(request.accessToken(), "merchant_logout");
		}
	}

	@Transactional
	public MerchantTokenRevokeResponse revokeMerchantTokens(Long merchantId) {
		MerchantAuth merchant = merchantAuthRepository.findByMerchantId(merchantId)
			.orElseThrow(() -> new AuthException("가맹점을 찾을 수 없습니다."));
		return new MerchantTokenRevokeResponse(merchantId, revokeActiveRefreshTokens(merchant));
	}

	@Transactional
	public MerchantStatusUpdateResponse updateMerchantStatus(
		Long merchantId,
		MerchantStatusUpdateRequest request
	) {
		if (request == null || request.status() == null) {
			throw new AuthException("가맹점 상태가 필요합니다.");
		}
		MerchantAuth merchant = merchantAuthRepository.findByMerchantId(merchantId)
			.orElseThrow(() -> new AuthException("가맹점을 찾을 수 없습니다."));

		merchant.setStatus(request.status());
		if (merchant.getStatus() != MerchantAccountStatus.ACTIVE) {
			revokeActiveRefreshTokens(merchant);
		}
		return new MerchantStatusUpdateResponse(merchantId, merchant.getStatus());
	}

	private int revokeActiveRefreshTokens(MerchantAuth merchant) {
		List<MerchantRefreshToken> activeTokens =
			merchantRefreshTokenRepository.findAllByAccountIdAndIsRevokedFalse(merchant.getAccountId());

		activeTokens.forEach(token -> token.setIsRevoked(true));
		redisTemplate.delete(RedisConfig.MERCHANT_REFRESH_KEY_PREFIX + merchant.getAccountId());
		return activeTokens.size();
	}

	private boolean shouldRotateRefreshToken(MerchantRefreshToken refreshToken) {
		LocalDateTime threshold = LocalDateTime.now()
			.plusNanos(TimeUnit.MILLISECONDS.toNanos(REFRESH_ROTATION_THRESHOLD_MILLIS));
		return !refreshToken.getExpiresAt().isAfter(threshold);
	}

	private String resolveRefreshToken(String authorization, String bodyRefreshToken) {
		if (authorization != null && authorization.startsWith("Bearer ")) {
			return authorization.substring(7);
		}
		if (bodyRefreshToken != null && !bodyRefreshToken.isBlank()) {
			return bodyRefreshToken;
		}
		throw new AuthException("Refresh Token이 필요합니다.");
	}

	private void blacklistAccessToken(String accessToken, String reason) {
		redisTemplate.opsForValue().set(
			RedisConfig.ACCESS_BLACKLIST_KEY_PREFIX + accessToken,
			reason,
			jwtProperties.accessTokenExpiration(),
			TimeUnit.MILLISECONDS
		);
	}

	private KakaoMerchantLoginResponse loginExistingMerchant(MerchantAuth merchant) {
		if (merchant.getStatus() == MerchantAccountStatus.SUSPENDED
			|| merchant.getStatus() == MerchantAccountStatus.WITHDRAWN
			|| merchant.getStatus() == MerchantAccountStatus.REJECTED) {
			throw new AuthException("로그인할 수 없는 가맹점 상태입니다.");
		}

		if (merchant.getStatus() == MerchantAccountStatus.DRAFT) {
			return issueSignupResponse(false, merchant);
		}
		if (merchant.getStatus() != MerchantAccountStatus.ACTIVE) {
			throw new AuthException("로그인할 수 없는 가맹점 상태입니다.");
		}

		merchant.setLastLoginAt(LocalDateTime.now());
		String accessToken = jwtService.createAccessToken(merchant.getAccountId(), JwtRole.MERCHANT);
		String refreshToken = jwtService.createRefreshToken(merchant.getAccountId(), JwtRole.MERCHANT);
		saveRefreshToken(merchant.getAccountId(), refreshToken);

		return new KakaoMerchantLoginResponse(
			false,
			merchant.getAccountId(),
			merchant.getMerchantId(),
			merchant.getStatus(),
			accessToken,
			refreshToken,
			JwtRole.MERCHANT,
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
		return issueSignupResponse(true, saved);
	}

	private KakaoMerchantLoginResponse issueSignupResponse(boolean isNewMerchant, MerchantAuth merchant) {
		// 신규/가입 미완료 가맹점은 정식 로그인 토큰 대신 SIGNUP 타입 JWT를 받습니다.
		String signupToken = jwtService.createSignupToken(merchant.getAccountId());
		return new KakaoMerchantLoginResponse(
			isNewMerchant,
			merchant.getAccountId(),
			merchant.getMerchantId(),
			merchant.getStatus(),
			null,
			null,
			null,
			signupToken
		);
	}

	private Long extractSignupAccountId(String authorization) {
		String signupToken = extractBearerToken(authorization);
		if (!jwtService.validateToken(signupToken)) {
			throw new AuthException("유효하지 않은 회원가입 토큰입니다.");
		}
		if (!JwtTokenType.SIGNUP.name().equals(jwtService.extractTokenType(signupToken))) {
			throw new AuthException("회원가입용 토큰이 아닙니다.");
		}
		if (!JwtRole.MERCHANT.name().equals(jwtService.extractRole(signupToken))) {
			throw new AuthException("가맹점 회원가입 토큰이 아닙니다.");
		}
		return jwtService.extractAccountId(signupToken);
	}

	private String extractBearerToken(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			throw new AuthException("Authorization 헤더에 회원가입 토큰이 필요합니다.");
		}
		return authorization.substring(7);
	}

	private MerchantAuth findSignupMerchant(Long accountId) {
		MerchantAuth merchant = merchantAuthRepository.findById(accountId)
			.orElseThrow(() -> new AuthException("가맹점 계정을 찾을 수 없습니다."));

		if (merchant.getStatus() != MerchantAccountStatus.DRAFT) {
			throw new AuthException("회원가입을 진행할 수 없는 가맹점 상태입니다.");
		}
		return merchant;
	}

	private void validateSignupRequest(MerchantSignupRequest request) {
		requireText(request.businessNumber(), "사업자번호는 필수입니다.");
		requireText(request.merchantName(), "가맹점명은 필수입니다.");
		requireText(request.mccCode(), "MCC 코드는 필수입니다.");
		requireText(request.representativeName(), "대표자명은 필수입니다.");
		requireText(request.contactEmail(), "담당자 이메일은 필수입니다.");
		requireText(request.contactPhone(), "담당자 연락처는 필수입니다.");
		requireText(request.settlementAccount(), "정산 계좌는 필수입니다.");
		requireText(request.bankName(), "은행명은 필수입니다.");
		requireText(request.serviceName(), "서비스명은 필수입니다.");
	}

	private void requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new AuthException(message);
		}
	}

	private MerchantCreateRequest toMerchantCreateRequest(MerchantSignupRequest request) {
		return new MerchantCreateRequest(
			request.businessNumber(),
			request.merchantName(),
			request.mccCode(),
			request.representativeName(),
			request.contactEmail(),
			request.contactPhone(),
			request.settlementAccount(),
			request.bankName(),
			request.serviceName()
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
