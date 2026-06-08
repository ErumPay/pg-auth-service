package com.erumpay.pg_auth_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenRefreshResponse(
	@JsonProperty("access_token")
	String accessToken,

	@JsonProperty("refresh_token")
	String refreshToken
) {
}
