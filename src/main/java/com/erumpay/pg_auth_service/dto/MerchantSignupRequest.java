package com.erumpay.pg_auth_service.dto;

public record MerchantSignupRequest(
	String businessName,                  // 상호명 또는 사업자명
	String businessRegistrationNumber,    // 사업자등록번호
	String representativeName             // 대표자명
) {
}
