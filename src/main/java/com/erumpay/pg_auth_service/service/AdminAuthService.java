package com.erumpay.pg_auth_service.service;

import com.erumpay.pg_auth_service.dto.AdminLoginRequest;
import com.erumpay.pg_auth_service.dto.AdminLoginResponse;
import com.erumpay.pg_auth_service.dto.AdminLogoutResponse;
import com.erumpay.pg_auth_service.entity.AdminAccount;
import com.erumpay.pg_auth_service.entity.AdminRefreshToken;
import com.erumpay.pg_auth_service.exception.AuthErrorCode;
import com.erumpay.pg_auth_service.exception.AuthException;
import com.erumpay.pg_auth_service.repository.AdminAccountRepository;
import com.erumpay.pg_auth_service.repository.AdminRefreshTokenRepository;
import com.erumpay.pg_auth_service.security.JwtProperties;
import com.erumpay.pg_auth_service.security.JwtRole;
import com.erumpay.pg_auth_service.security.JwtService;
import com.erumpay.pg_auth_service.security.JwtTokenType;
import com.erumpay.pg_auth_service.security.TotpService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

	private static final int MAX_FAILED_LOGIN_COUNT = 5;
	private static final int LOCK_MINUTES = 10;

	private final AdminAccountRepository adminAccountRepository;
	private final AdminRefreshTokenRepository adminRefreshTokenRepository;
	private final AdminAuditLogService adminAuditLogService;
	private final PasswordEncoder passwordEncoder;
	private final TotpService totpService;
	private final JwtService jwtService;
	private final JwtProperties jwtProperties;

	@Value("${admin.auth.totp-enabled:true}")
	private boolean totpEnabled;

	@Transactional
	public AdminLoginResponse login(AdminLoginRequest request, String ipAddress) {
		if (isBlank(request.loginId()) || isBlank(request.password())
			|| (totpEnabled && isBlank(request.totpCode()))) {
			throw new AuthException(AuthErrorCode.ADMIN_LOGIN_REQUEST_INVALID);
		}

		AdminAccount admin = adminAccountRepository.findByLoginId(request.loginId())
			.orElseThrow(() -> new AuthException(AuthErrorCode.ADMIN_CREDENTIALS_INVALID));

		validateLock(admin);
		validateAllowedIp(admin, ipAddress);
		validatePassword(admin, request.password());
		validateTotp(admin, request.totpCode());

		admin.setFailedLoginCount(0);
		admin.setLockedUntil(null);
		admin.setLastLoginAt(LocalDateTime.now());

		String accessToken = jwtService.createAccessToken(admin.getAdminId(), JwtRole.PG_ADMIN);
		String refreshToken = jwtService.createRefreshToken(admin.getAdminId(), JwtRole.PG_ADMIN);
		saveRefreshToken(admin.getAdminId(), refreshToken);
		adminAuditLogService.record(admin.getAdminId(), "LOGIN", String.valueOf(admin.getAdminId()), "{}", ipAddress);

		return new AdminLoginResponse(accessToken, refreshToken, JwtRole.PG_ADMIN);
	}

	@Transactional
	public AdminLogoutResponse logout(String authorization, String ipAddress) {
		String refreshToken = extractBearerToken(authorization);
		if (!jwtService.validateToken(refreshToken)
			|| !JwtTokenType.REFRESH.name().equals(jwtService.extractTokenType(refreshToken))
			|| !JwtRole.PG_ADMIN.name().equals(jwtService.extractRole(refreshToken))) {
			throw new AuthException(AuthErrorCode.ADMIN_REFRESH_TOKEN_INVALID);
		}

		Long adminId = jwtService.extractAccountId(refreshToken);
		AdminRefreshToken token = adminRefreshTokenRepository.findByTokenHashAndIsRevokedFalse(hashToken(refreshToken))
			.orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_REVOKED));
		token.setIsRevoked(true);
		adminAuditLogService.record(adminId, "LOGOUT", String.valueOf(adminId), "{}", ipAddress);
		return new AdminLogoutResponse("로그아웃 완료");
	}

	private void validateLock(AdminAccount admin) {
		if (admin.getLockedUntil() != null && admin.getLockedUntil().isAfter(LocalDateTime.now())) {
			throw new AuthException(AuthErrorCode.ADMIN_ACCOUNT_LOCKED);
		}
	}

	private void validateAllowedIp(AdminAccount admin, String ipAddress) {
		if (isBlank(admin.getAllowedIps())) {
			return;
		}
		boolean allowed = Arrays.stream(admin.getAllowedIps().split(","))
			.map(String::trim)
			.anyMatch(ip -> ip.equals(ipAddress));
		if (!allowed) {
			throw new AuthException(AuthErrorCode.ADMIN_IP_NOT_ALLOWED);
		}
	}

	private void validatePassword(AdminAccount admin, String password) {
		if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
			increaseFailure(admin);
			throw new AuthException(AuthErrorCode.ADMIN_CREDENTIALS_INVALID);
		}
	}

	private void validateTotp(AdminAccount admin, String totpCode) {
		if (!totpEnabled) {
			return;
		}
		if (!totpService.verify(admin.getTotpSecret(), totpCode)) {
			increaseFailure(admin);
			throw new AuthException(AuthErrorCode.ADMIN_TOTP_INVALID);
		}
	}

	private void increaseFailure(AdminAccount admin) {
		int failedCount = admin.getFailedLoginCount() == null ? 0 : admin.getFailedLoginCount();
		admin.setFailedLoginCount(failedCount + 1);
		if (admin.getFailedLoginCount() >= MAX_FAILED_LOGIN_COUNT) {
			admin.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
		}
	}

	private void saveRefreshToken(Long adminId, String refreshToken) {
		AdminRefreshToken token = new AdminRefreshToken();
		token.setAdminId(adminId);
		token.setTokenHash(hashToken(refreshToken));
		token.setExpiresAt(LocalDateTime.ofInstant(
			Instant.now().plusMillis(jwtProperties.adminRefreshTokenExpiration()),
			ZoneId.systemDefault()
		));
		token.setIsRevoked(false);
		adminRefreshTokenRepository.save(token);
	}

	private String extractBearerToken(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			throw new AuthException(AuthErrorCode.REFRESH_TOKEN_REQUIRED);
		}
		return authorization.substring(7);
	}

	private String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new AuthException(AuthErrorCode.TOKEN_HASH_FAILED, ex);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
