package com.erumpay.pg_auth_service.controller;

import com.erumpay.pg_auth_service.dto.AdminAuditLogDetailResponse;
import com.erumpay.pg_auth_service.service.AdminAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pg-admin/audit-logs")
public class PgAdminAuditLogController {

	private final AdminAuditLogService adminAuditLogService;

	@GetMapping
	public Page<AdminAuditLogDetailResponse> getLogs(
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return adminAuditLogService.getLogs(pageable);
	}
}
