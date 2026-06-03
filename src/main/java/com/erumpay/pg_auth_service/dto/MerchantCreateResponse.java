package com.erumpay.pg_auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MerchantCreateResponse(
	@JsonProperty("merchant_id")
	Long merchantId,

	@JsonProperty("review_status")
	String reviewStatus
) {
}
