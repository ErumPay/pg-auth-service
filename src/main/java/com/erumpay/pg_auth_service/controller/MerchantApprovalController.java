package com.erumpay.pg_auth_service.controller;

import com.erumpay.pg_auth_service.dto.MerchantApprovalResponse;
import com.erumpay.pg_auth_service.service.MerchantApprovalService;
import lombok.RequiredArgsConstructor;
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
	public MerchantApprovalResponse approve(@PathVariable Long merchantId) {
		return merchantApprovalService.approve(merchantId);
	}
}
