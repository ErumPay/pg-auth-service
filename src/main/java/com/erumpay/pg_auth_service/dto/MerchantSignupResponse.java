package com.erumpay.pg_auth_service.dto;

import com.erumpay.pg_auth_service.entity.MerchantAccountStatus;

public record MerchantSignupResponse(
	Long accountId,
	MerchantAccountStatus status
) {
}
