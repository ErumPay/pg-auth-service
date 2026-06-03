package com.erumpay.pg_auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
	Long id,
	@JsonProperty("kakao_account")
	KakaoAccount kakaoAccount
) {
	public record KakaoAccount(
		Profile profile
	) {
	}

	public record Profile(
		String nickname
	) {
	}
}
