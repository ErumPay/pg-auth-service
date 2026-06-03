package com.erumpay.pg_auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminAuditLogResponse(
	@JsonProperty("log_id")
	Long logId
) {
}
