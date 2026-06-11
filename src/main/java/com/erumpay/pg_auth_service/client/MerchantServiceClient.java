package com.erumpay.pg_auth_service.client;

import com.erumpay.pg_auth_service.dto.MerchantCreateRequest;
import com.erumpay.pg_auth_service.dto.MerchantCreateResponse;
import com.erumpay.pg_auth_service.dto.MerchantServiceStatusResponse;
import com.erumpay.pg_auth_service.dto.MerchantStatusUpdateRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "merchant-service", url = "${merchant-service.url}")
public interface MerchantServiceClient {

	@PostMapping("/internal/v1/merchants")
	MerchantCreateResponse createMerchant(
		@RequestHeader("Idempotency-Key") String idempotencyKey,
		@RequestBody MerchantCreateRequest request
	);

	@PatchMapping("/internal/v1/merchants/{merchantId}/status")
	MerchantServiceStatusResponse updateMerchantStatus(
		@PathVariable Long merchantId,
		@RequestBody MerchantStatusUpdateRequest request
	);
}
