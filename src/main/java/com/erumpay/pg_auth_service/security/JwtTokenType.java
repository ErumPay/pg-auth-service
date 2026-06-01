package com.erumpay.pg_auth_service.security;

public enum JwtTokenType {
	ACCESS,   // 실제 API 접근용 토큰
	REFRESH,  // Access Token 재발급용 토큰
	SIGNUP    // 신규 가맹점이 약관 동의/추가정보 입력할 때 쓰는 임시 회원가입 토큰
}
