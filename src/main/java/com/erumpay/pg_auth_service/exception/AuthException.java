package com.erumpay.pg_auth_service.exception;

// 인증 도메인에서 잘못된 요청이나 토큰 오류가 발생했을 때 사용하는 예외입니다.
public class AuthException extends RuntimeException {

	public AuthException(String message) {
		super(message);
	}
}
