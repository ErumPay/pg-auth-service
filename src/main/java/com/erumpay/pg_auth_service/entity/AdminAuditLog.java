package com.erumpay.pg_auth_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// PG 관리자 작업 이력을 저장하는 감사 로그 엔티티입니다.
@Getter
@Setter
@Entity
@Table(name = "pg_admin_audit_logs")
@NoArgsConstructor
public class AdminAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "log_id")
	private Long logId;

	@Column(name = "admin_id", nullable = false)
	private Long adminId;

	@Column(nullable = false, length = 100)
	private String action;

	@Column(name = "target_id", length = 100)
	private String targetId;

	@Lob
	@Column(name = "change_detail")
	private String changeDetail;

	@Column(name = "ip_address", nullable = false, length = 50)
	private String ipAddress;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}
}
