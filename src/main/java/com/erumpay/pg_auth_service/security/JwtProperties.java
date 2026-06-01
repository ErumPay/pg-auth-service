package com.erumpay.pg_auth_service.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yml의 jwt.* 설정을 Java 객체로 바인딩합니다.
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
	String secret,
	long accessTokenExpiration,
	long refreshTokenExpiration,
	long signupTokenExpiration // 신규 가맹점 회원가입 절차용 임시 JWT 만료 시간
) {
}
