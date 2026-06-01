package com.erumpay.pg_auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminLoginRequest(
	@JsonProperty("login_id")
	String loginId,

	String password,

	@JsonProperty("totp_code")
	String totpCode
) {
}
