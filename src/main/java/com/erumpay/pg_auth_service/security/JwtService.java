package com.erumpay.pg_auth_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private static final String CLAIM_ACCOUNT_ID = "accountId";
	private static final String CLAIM_MERCHANT_ID = "merchantId";
	private static final String CLAIM_ROLE = "role";
	private static final String CLAIM_TOKEN_TYPE = "tokenType";

	private final JwtProperties jwtProperties;
	private final SecretKey secretKey;

	public JwtService(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
		this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String createAccessToken(Long accountId, JwtRole role) {
		return createToken(accountId, null, role, JwtTokenType.ACCESS, jwtProperties.accessTokenExpiration());
	}

	public String createMerchantAccessToken(Long accountId, Long merchantId) {
		return createToken(
			accountId,
			merchantId,
			JwtRole.MERCHANT,
			JwtTokenType.ACCESS,
			jwtProperties.accessTokenExpiration()
		);
	}

	public String createRefreshToken(Long accountId, JwtRole role) {
		long expiration = role == JwtRole.PG_ADMIN
			? jwtProperties.adminRefreshTokenExpiration()
			: jwtProperties.refreshTokenExpiration();
		return createToken(accountId, null, role, JwtTokenType.REFRESH, expiration);
	}

	public String createSignupToken(Long accountId) {
		return createToken(
			accountId,
			null,
			JwtRole.MERCHANT,
			JwtTokenType.SIGNUP,
			jwtProperties.signupTokenExpiration()
		);
	}

	public boolean validateToken(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public Long extractAccountId(String token) {
		return parseClaims(token).get(CLAIM_ACCOUNT_ID, Long.class);
	}

	public String extractRole(String token) {
		return parseClaims(token).get(CLAIM_ROLE, String.class);
	}

	public Long extractMerchantId(String token) {
		return parseClaims(token).get(CLAIM_MERCHANT_ID, Long.class);
	}

	public String extractTokenType(String token) {
		return parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class);
	}

	private String createToken(
		Long accountId,
		Long merchantId,
		JwtRole role,
		JwtTokenType tokenType,
		long expirationMillis
	) {
		Date now = new Date();
		Date expiresAt = new Date(now.getTime() + expirationMillis);

		var builder = Jwts.builder()
			.claim(CLAIM_ACCOUNT_ID, accountId)
			.claim(CLAIM_ROLE, role.name())
			.claim(CLAIM_TOKEN_TYPE, tokenType.name())
			.issuedAt(now)
			.expiration(expiresAt);
		if (merchantId != null) {
			builder.claim(CLAIM_MERCHANT_ID, merchantId);
		}
		return builder.signWith(secretKey).compact();
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}
}
