package com.erumpay.pg_auth_service.config;

import com.erumpay.pg_auth_service.entity.AdminAccount;
import com.erumpay.pg_auth_service.repository.AdminAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DevAdminBootstrap implements ApplicationRunner {

	private final AdminAccountRepository adminAccountRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${admin.bootstrap.enabled:false}")
	private boolean enabled;

	@Value("${admin.bootstrap.login-id:admin}")
	private String loginId;

	@Value("${admin.bootstrap.password:}")
	private String password;

	@Value("${admin.bootstrap.totp-secret:}")
	private String totpSecret;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!enabled || loginId.isBlank() || password.isBlank()
			|| adminAccountRepository.findByLoginId(loginId).isPresent()) {
			return;
		}

		AdminAccount admin = new AdminAccount();
		admin.setLoginId(loginId);
		admin.setPasswordHash(passwordEncoder.encode(password));
		admin.setTotpSecret(totpSecret.isBlank() ? null : totpSecret);
		admin.setFailedLoginCount(0);
		adminAccountRepository.save(admin);
	}
}
