package com.erumpay.pg_auth_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 가맹점 계정이 어떤 약관에 동의했는지 저장하는 엔티티입니다.
@Getter
@Setter
@Entity
@Table(name = "pg_merchant_terms_agreements")
@NoArgsConstructor
public class MerchantTermsAgreement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "agreement_id")
	private Long agreementId;

	@Column(name = "account_id", nullable = false)
	private Long accountId;

	@Column(name = "service_terms_agreed", nullable = false)
	private Boolean serviceTermsAgreed;

	@Column(name = "privacy_policy_agreed", nullable = false)
	private Boolean privacyPolicyAgreed;

	@Column(name = "marketing_agreed", nullable = false)
	private Boolean marketingAgreed;

	@Column(name = "terms_version", nullable = false, length = 20)
	private String termsVersion;

	@Column(name = "agreed_ip", length = 50)
	private String agreedIp;

	@Column(name = "agreed_at")
	private LocalDateTime agreedAt;

	@PrePersist
	void prePersist() {
		if (agreedAt == null) {
			agreedAt = LocalDateTime.now();
		}
	}
}
