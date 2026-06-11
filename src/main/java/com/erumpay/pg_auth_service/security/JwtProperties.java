package com.erumpay.pg_auth_service.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
	String secret,
	long accessTokenExpiration,
	long refreshTokenExpiration,
	long adminRefreshTokenExpiration,
	long signupTokenExpiration
) {
}
