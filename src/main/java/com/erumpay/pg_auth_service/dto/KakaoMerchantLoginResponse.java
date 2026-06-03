package com.erumpay.pg_auth_service.dto;

import com.erumpay.pg_auth_service.entity.MerchantAccountStatus;
import com.erumpay.pg_auth_service.security.JwtRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KakaoMerchantLoginResponse(
	@JsonProperty("is_new_merchant")
	boolean isNewMerchant,

	@JsonProperty("account_id")
	Long accountId,

	@JsonProperty("merchant_id")
	Long merchantId,

	MerchantAccountStatus status,

	@JsonProperty("access_token")
	String accessToken,

	@JsonProperty("refresh_token")
	String refreshToken,

	JwtRole role,

	@JsonProperty("signup_token")
	String signupToken
) {
}
