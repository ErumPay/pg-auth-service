package com.erumpay.pg_auth_service.repository;

import com.erumpay.pg_auth_service.entity.MerchantTermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantTermsAgreementRepository extends JpaRepository<MerchantTermsAgreement, Long> {
}
