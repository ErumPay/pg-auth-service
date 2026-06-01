package com.erumpay.pg_auth_service.client;

import com.erumpay.pg_auth_service.dto.KakaoTokenResponse;
import com.erumpay.pg_auth_service.dto.KakaoUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

// 카카오 OAuth 서버와 통신하는 전용 클라이언트입니다.
@Component
@RequiredArgsConstructor
public class KakaoClient {

	private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
	private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

	private final KakaoProperties kakaoProperties;
	private final RestTemplate restTemplate = new RestTemplate();

	public KakaoTokenResponse requestToken(String authorizationCode) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "authorization_code");
		body.add("client_id", kakaoProperties.clientId());
		// 카카오 Client Secret을 사용하는 경우에만 요청 body에 포함합니다.
		// Developers에서 Client Secret 사용 안 함으로 설정했다면 빈 값을 보내지 않는 편이 안전합니다.
		if (kakaoProperties.clientSecret() != null && !kakaoProperties.clientSecret().isBlank()) {
			body.add("client_secret", kakaoProperties.clientSecret());
		}
		body.add("redirect_uri", kakaoProperties.redirectUri());
		body.add("code", authorizationCode);

		return restTemplate.postForObject(
			KAKAO_TOKEN_URL,
			new HttpEntity<>(body, headers),
			KakaoTokenResponse.class
		);
	}

	public KakaoUserResponse requestUserInfo(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);

		return restTemplate.postForObject(
			KAKAO_USER_INFO_URL,
			new HttpEntity<>(headers),
			KakaoUserResponse.class
		);
	}
}
