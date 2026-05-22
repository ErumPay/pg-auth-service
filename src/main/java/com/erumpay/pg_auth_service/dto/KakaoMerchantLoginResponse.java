package com.erumpay.pg_auth_service.dto;

import com.erumpay.pg_auth_service.entity.MerchantAccountStatus;

public record KakaoMerchantLoginResponse(
	boolean isNewMerchant,
	Long accountId,
	MerchantAccountStatus status,
	String accessToken,
	String refreshToken,
	String signupToken
) {
}
