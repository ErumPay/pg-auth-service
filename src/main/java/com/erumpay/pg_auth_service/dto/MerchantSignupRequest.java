package com.erumpay.pg_auth_service.dto;

public record MerchantSignupRequest(
	Long accountId,
	String businessName,
	String businessRegistrationNumber,
	String representativeName
) {
}
