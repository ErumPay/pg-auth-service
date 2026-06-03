package com.erumpay.pg_auth_service.service;

import com.erumpay.pg_auth_service.dto.AdminAuditLogRequest;
import com.erumpay.pg_auth_service.dto.AdminAuditLogResponse;
import com.erumpay.pg_auth_service.entity.AdminAuditLog;
import com.erumpay.pg_auth_service.exception.AuthException;
import com.erumpay.pg_auth_service.repository.AdminAccountRepository;
import com.erumpay.pg_auth_service.repository.AdminAuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

	private final AdminAccountRepository adminAccountRepository;
	private final AdminAuditLogRepository adminAuditLogRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public AdminAuditLogResponse record(AdminAuditLogRequest request) {
		validate(request);
		if (!adminAccountRepository.existsById(request.adminId())) {
			throw new AuthException(HttpStatus.NOT_FOUND, "존재하지 않는 admin_id입니다.");
		}

		AdminAuditLog saved = adminAuditLogRepository.save(createLog(
			request.adminId(),
			request.action(),
			request.targetId(),
			toJson(request.changeDetail()),
			request.ipAddress()
		));
		return new AdminAuditLogResponse(saved.getLogId());
	}

	@Transactional
	public AdminAuditLog record(Long adminId, String action, String targetId, String changeDetail, String ipAddress) {
		return adminAuditLogRepository.save(createLog(adminId, action, targetId, changeDetail, ipAddress));
	}

	private AdminAuditLog createLog(
		Long adminId,
		String action,
		String targetId,
		String changeDetail,
		String ipAddress
	) {
		AdminAuditLog log = new AdminAuditLog();
		log.setAdminId(adminId);
		log.setAction(action);
		log.setTargetId(targetId);
		log.setChangeDetail(changeDetail);
		log.setIpAddress(ipAddress == null || ipAddress.isBlank() ? "UNKNOWN" : ipAddress);
		return log;
	}

	private void validate(AdminAuditLogRequest request) {
		if (request.adminId() == null) {
			throw new AuthException(HttpStatus.BAD_REQUEST, "admin_id는 필수입니다.");
		}
		if (isBlank(request.action())) {
			throw new AuthException(HttpStatus.BAD_REQUEST, "action은 필수입니다.");
		}
		if (isBlank(request.ipAddress())) {
			throw new AuthException(HttpStatus.BAD_REQUEST, "ip_address는 필수입니다.");
		}
	}

	private String toJson(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException ex) {
			throw new AuthException(HttpStatus.BAD_REQUEST, "change_detail JSON 변환에 실패했습니다.");
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
