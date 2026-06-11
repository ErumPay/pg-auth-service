package com.erumpay.pg_auth_service.controller;

import com.erumpay.pg_auth_service.dto.MerchantApprovalResponse;
import com.erumpay.pg_auth_service.service.MerchantApprovalService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pg-admin/merchants")
public class MerchantApprovalController {

	private final MerchantApprovalService merchantApprovalService;

	@PatchMapping("/{merchantId}/approve")
	public MerchantApprovalResponse approve(
		@PathVariable Long merchantId,
		Authentication authentication,
		HttpServletRequest request
	) {
		return merchantApprovalService.approve(
			merchantId,
			(Long) authentication.getPrincipal(),
			clientIp(request)
		);
	}

	private String clientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		return forwardedFor == null || forwardedFor.isBlank()
			? request.getRemoteAddr()
			: forwardedFor.split(",")[0].trim();
	}
}
