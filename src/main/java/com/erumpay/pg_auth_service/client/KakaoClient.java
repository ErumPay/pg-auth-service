package com.erumpay.pg_auth_service.client;

import com.erumpay.pg_auth_service.dto.KakaoTokenResponse;
import com.erumpay.pg_auth_service.dto.KakaoUserResponse;
import com.erumpay.pg_auth_service.exception.AuthErrorCode;
import com.erumpay.pg_auth_service.exception.AuthException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
public class KakaoClient {

	private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
	private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

	private final KakaoProperties kakaoProperties;
	private final RestTemplate restTemplate;

	public KakaoClient(KakaoProperties kakaoProperties) {
		this.kakaoProperties = kakaoProperties;
		this.restTemplate = createRestTemplate();
	}

	public KakaoTokenResponse requestToken(String authorizationCode) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "authorization_code");
		body.add("client_id", kakaoProperties.clientId());
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
			throw new AuthException(AuthErrorCode.KAKAO_TOKEN_REQUEST_REJECTED);
		} catch (RestClientException ex) {
			throw new AuthException(AuthErrorCode.KAKAO_TOKEN_REQUEST_UNAVAILABLE);
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
			throw new AuthException(AuthErrorCode.KAKAO_USER_REQUEST_REJECTED);
		} catch (RestClientException ex) {
			throw new AuthException(AuthErrorCode.KAKAO_USER_REQUEST_UNAVAILABLE);
		}
	}

	private RestTemplate createRestTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(3000);
		factory.setReadTimeout(5000);
		return new RestTemplate(factory);
	}
}
