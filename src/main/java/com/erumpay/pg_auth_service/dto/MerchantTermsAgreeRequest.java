package com.erumpay.pg_auth_service.dto;

public record MerchantTermsAgreeRequest(
	Long accountId,
	Boolean serviceTermsAgreed,
	Boolean privacyPolicyAgreed,
	Boolean marketingAgreed,
	String termsVersion
) {
}
