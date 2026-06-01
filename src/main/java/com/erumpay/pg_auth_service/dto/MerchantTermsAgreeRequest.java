package com.erumpay.pg_auth_service.dto;

public record MerchantTermsAgreeRequest(
	Boolean serviceTermsAgreed,   // 서비스 이용약관 동의 여부, 필수 true
	Boolean privacyPolicyAgreed,  // 개인정보 처리방침 동의 여부, 필수 true
	Boolean marketingAgreed,      // 마케팅 수신 동의 여부, 선택
	String termsVersion           // 동의한 약관 버전, 예: v1.0
) {
}
