package com.erumpay.pg_auth_service.repository;

import com.erumpay.pg_auth_service.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
}
