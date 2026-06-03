package com.erumpay.pg_auth_service.dto;

public record LogoutRequest(
	String accessToken,
	String refreshToken
) {
}
