package com.erumpay.pg_auth_service.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yml의 kakao.* 설정을 담는 객체입니다.
@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(
	String clientId,
	String clientSecret,
	String redirectUri
) {
}
