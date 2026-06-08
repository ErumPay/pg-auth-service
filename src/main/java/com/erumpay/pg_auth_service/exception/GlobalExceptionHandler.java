package com.erumpay.pg_auth_service.exception;

import feign.FeignException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
		return toResponse(ex.getErrorCode());
	}

	@ExceptionHandler({
		HttpMessageNotReadableException.class,
		MethodArgumentTypeMismatchException.class,
		NoResourceFoundException.class
	})
	public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception ex) {
		return toResponse(AuthErrorCode.INVALID_REQUEST);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
		List<FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
			.map(this::toDetail)
			.toList();
		AuthErrorCode errorCode = AuthErrorCode.INVALID_REQUEST;
		return ResponseEntity.status(errorCode.getStatus())
			.body(new ErrorResponse(
				errorCode.getStatus().value(),
				errorCode.getStatus().name(),
				errorCode.getCode(),
				errorCode.getReason(),
				errorCode.getMessage(),
				details
			));
	}

	@ExceptionHandler(FeignException.class)
	public ResponseEntity<ErrorResponse> handleFeignException(FeignException ex) {
		log.error("External merchant-service error", ex);
		return toResponse(AuthErrorCode.MERCHANT_CREATE_REQUEST_FAILED);
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex) {
		log.error("Auth data access error", ex);
		return toResponse(AuthErrorCode.AUTH_DATA_ACCESS_FAILED);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception ex) {
		log.error("Unhandled server error", ex);
		return toResponse(AuthErrorCode.INTERNAL_SERVER_ERROR);
	}

	private ResponseEntity<ErrorResponse> toResponse(AuthErrorCode errorCode) {
		return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.from(errorCode));
	}

	private FieldErrorDetail toDetail(FieldError fieldError) {
		return new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
	}
}
