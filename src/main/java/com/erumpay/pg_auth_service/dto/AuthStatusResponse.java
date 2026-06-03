package com.erumpay.pg_auth_service.dto;

public record AuthStatusResponse(
	String service,
	String status
) {
}
