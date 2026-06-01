package com.erumpay.pg_auth_service.dto;

import com.erumpay.pg_auth_service.security.JwtRole;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminLoginResponse(
	@JsonProperty("access_token")
	String accessToken,

	@JsonProperty("refresh_token")
	String refreshToken,

	JwtRole role
) {
}
