package com.erumpay.pg_auth_service.exception;

import java.util.List;

public record ErrorResponse(
	int status,
	String error,
	String code,
	String reason,
	String message,
	List<FieldErrorDetail> details
) {

	public static ErrorResponse from(AuthErrorCode errorCode) {
		return new ErrorResponse(
			errorCode.getStatus().value(),
			errorCode.getStatus().name(),
			errorCode.getCode(),
			errorCode.getReason(),
			errorCode.getMessage(),
			List.of()
		);
	}
}
