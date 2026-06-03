package com.erumpay.pg_auth_service.controller;

import com.erumpay.pg_auth_service.dto.AdminAuditLogRequest;
import com.erumpay.pg_auth_service.dto.AdminAuditLogResponse;
import com.erumpay.pg_auth_service.service.AdminAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/auth")
public class AdminAuditLogController {

	private final AdminAuditLogService adminAuditLogService;

	@PostMapping("/audit-logs")
	public AdminAuditLogResponse record(@RequestBody AdminAuditLogRequest request) {
		return adminAuditLogService.record(request);
	}
}
