package com.erumpay.pg_auth_service.dto;

import com.erumpay.pg_auth_service.entity.MerchantAccountStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MerchantServiceStatusResponse(
	@JsonProperty("merchant_id")
	@JsonAlias("merchantId")
	Long merchantId,
	MerchantAccountStatus status
) {
}
