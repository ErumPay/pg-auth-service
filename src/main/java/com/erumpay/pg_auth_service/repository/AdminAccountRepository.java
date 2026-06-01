package com.erumpay.pg_auth_service.repository;

import com.erumpay.pg_auth_service.entity.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, Long> {
}
