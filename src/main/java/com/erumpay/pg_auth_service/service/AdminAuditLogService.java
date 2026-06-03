package com.erumpay.pg_auth_service.service;

import com.erumpay.pg_auth_service.entity.AdminAuditLog;
import com.erumpay.pg_auth_service.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

	private final AdminAuditLogRepository adminAuditLogRepository;

	public AdminAuditLog record(Long adminId, String action, String targetId, String changeDetail, String ipAddress) {
		AdminAuditLog log = new AdminAuditLog();
		log.setAdminId(adminId);
		log.setAction(action);
		log.setTargetId(targetId);
		log.setChangeDetail(changeDetail);
		log.setIpAddress(ipAddress == null || ipAddress.isBlank() ? "UNKNOWN" : ipAddress);
		return adminAuditLogRepository.save(log);
	}
}
