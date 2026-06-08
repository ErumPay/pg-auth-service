package com.erumpay.pg_auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record AdminAuditLogRequest(
	@JsonProperty("admin_id")
	Long adminId,

	String action,

	@JsonProperty("target_id")
	String targetId,

	@JsonProperty("change_detail")
	Map<String, Object> changeDetail,

	@JsonProperty("ip_address")
	String ipAddress
) {
}
