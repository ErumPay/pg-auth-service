package com.erumpay.pg_auth_service.dto;

public record MerchantTermsAgreeResponse(
	Long agreementId,
	Long accountId,
	boolean agreed
) {
}
