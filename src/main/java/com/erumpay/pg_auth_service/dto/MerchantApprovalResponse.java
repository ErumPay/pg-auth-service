package com.erumpay.pg_auth_service.dto;

import com.erumpay.pg_auth_service.entity.MerchantAccountStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MerchantApprovalResponse(
	@JsonProperty("merchant_id")
	Long merchantId,
	MerchantAccountStatus status
) {
}
