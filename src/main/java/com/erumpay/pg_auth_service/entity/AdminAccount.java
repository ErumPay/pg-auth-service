package com.erumpay.pg_auth_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// PG 관리자 계정입니다. 이번 범위에서는 Service/Controller 없이 저장 구조만 둡니다.
@Getter
@Setter
@Entity
@Table(name = "pg_admin_accounts")
@NoArgsConstructor
public class AdminAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "admin_id")
	private Long adminId;

	@Column(name = "login_id", nullable = false, unique = true, length = 50)
	private String loginId;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "totp_secret")
	private String totpSecret;

	@Lob
	@Column(name = "allowed_ips")
	private String allowedIps;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	@Column(name = "locked_until")
	private LocalDateTime lockedUntil;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "failed_login_count", nullable = false)
	private Integer failedLoginCount = 0;

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
		if (failedLoginCount == null) {
			failedLoginCount = 0;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
