package com.erumpay.pg_auth_service.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

	private final HttpStatus status;

	public AuthException(String message) {
		this(HttpStatus.BAD_REQUEST, message);
	}

	public AuthException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
