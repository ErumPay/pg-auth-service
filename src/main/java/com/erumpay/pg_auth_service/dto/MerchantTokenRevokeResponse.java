package com.erumpay.pg_auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MerchantTokenRevokeResponse(
	@JsonProperty("merchant_id")
	Long merchantId,

	@JsonProperty("revoked_count")
	int revokedCount
) {
}
