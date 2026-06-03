package com.erumpay.pg_auth_service.dto;

import com.erumpay.pg_auth_service.entity.MerchantAccountStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MerchantSignupResponse(
	@JsonProperty("merchant_id")
	Long merchantId,

	MerchantAccountStatus status,

	@JsonProperty("review_status")
	String reviewStatus
) {
}
