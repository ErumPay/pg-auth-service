package com.erumpay.pg_auth_service.dto;

import com.erumpay.pg_auth_service.entity.AdminAuditLog;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record AdminAuditLogDetailResponse(
	@JsonProperty("log_id")
	Long logId,
	@JsonProperty("admin_id")
	Long adminId,
	String action,
	@JsonProperty("target_id")
	String targetId,
	@JsonProperty("change_detail")
	String changeDetail,
	@JsonProperty("ip_address")
	String ipAddress,
	@JsonProperty("created_at")
	LocalDateTime createdAt
) {
	public static AdminAuditLogDetailResponse from(AdminAuditLog log) {
		return new AdminAuditLogDetailResponse(
			log.getLogId(),
			log.getAdminId(),
			log.getAction(),
			log.getTargetId(),
			log.getChangeDetail(),
			log.getIpAddress(),
			log.getCreatedAt()
		);
	}
}
