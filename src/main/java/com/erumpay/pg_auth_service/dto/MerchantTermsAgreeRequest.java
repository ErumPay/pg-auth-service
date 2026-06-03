package com.erumpay.pg_auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MerchantTermsAgreeRequest(
	@JsonProperty("service_terms_agreed")
	Boolean serviceTermsAgreed,

	@JsonProperty("privacy_policy_agreed")
	Boolean privacyPolicyAgreed,

	@JsonProperty("marketing_agreed")
	Boolean marketingAgreed,

	@JsonProperty("terms_version")
	String termsVersion
) {
}
