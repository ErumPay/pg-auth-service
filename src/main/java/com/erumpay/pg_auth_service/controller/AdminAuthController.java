package com.erumpay.pg_auth_service.controller;

import com.erumpay.pg_auth_service.dto.AdminLoginRequest;
import com.erumpay.pg_auth_service.dto.AdminLoginResponse;
import com.erumpay.pg_auth_service.dto.AdminLogoutResponse;
import com.erumpay.pg_auth_service.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/admin")
public class AdminAuthController {

	private final AdminAuthService adminAuthService;

	@PostMapping("/login")
	public AdminLoginResponse login(
		@RequestBody AdminLoginRequest request,
		HttpServletRequest httpServletRequest
	) {
		return adminAuthService.login(request, clientIp(httpServletRequest));
	}

	@PostMapping("/logout")
	public AdminLogoutResponse logout(
		@RequestHeader("Authorization") String authorization,
		HttpServletRequest httpServletRequest
	) {
		return adminAuthService.logout(authorization, clientIp(httpServletRequest));
	}

	private String clientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
