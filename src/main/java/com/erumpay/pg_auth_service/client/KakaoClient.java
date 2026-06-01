package com.erumpay.pg_auth_service.client;

import com.erumpay.pg_auth_service.dto.KakaoTokenResponse;
import com.erumpay.pg_auth_service.dto.KakaoUserResponse;
import com.erumpay.pg_auth_service.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

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
		// 카카오 Developers에서 Client Secret 사용 설정을 켠 경우에만 전송합니다.
		// 사용하지 않는데 임의 값을 보내면 카카오 토큰 요청이 실패할 수 있습니다.
		if (kakaoProperties.clientSecret() != null && !kakaoProperties.clientSecret().isBlank()) {
			body.add("client_secret", kakaoProperties.clientSecret());
		}
		body.add("redirect_uri", kakaoProperties.redirectUri());
		body.add("code", authorizationCode);

		try {
			return restTemplate.postForObject(
				KAKAO_TOKEN_URL,
				new HttpEntity<>(body, headers),
				KakaoTokenResponse.class
			);
		} catch (RestClientResponseException ex) {
			throw new AuthException("카카오 토큰 요청 실패(" + ex.getStatusCode() + "): " + ex.getResponseBodyAsString());
		}
	}

	public KakaoUserResponse requestUserInfo(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);

		try {
			return restTemplate.postForObject(
				KAKAO_USER_INFO_URL,
				new HttpEntity<>(headers),
				KakaoUserResponse.class
			);
		} catch (RestClientResponseException ex) {
			throw new AuthException("카카오 사용자 정보 요청 실패(" + ex.getStatusCode() + "): " + ex.getResponseBodyAsString());
		}
	}
}
