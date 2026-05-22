package com.erumpay.pg_auth_service.dto;

public record TokenRefreshRequest(
	String refreshToken
) {
}
