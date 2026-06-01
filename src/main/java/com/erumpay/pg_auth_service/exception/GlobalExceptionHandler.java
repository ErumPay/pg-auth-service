package com.erumpay.pg_auth_service.exception;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<Map<String, String>> handleAuthException(AuthException ex) {
		return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleException(Exception ex) {
		log.error("Unhandled server error", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(Map.of(
				"message", "서버 오류가 발생했습니다.",
				"error", ex.getClass().getSimpleName(),
				"detail", ex.getMessage() == null ? "" : ex.getMessage()
			));
	}
}
