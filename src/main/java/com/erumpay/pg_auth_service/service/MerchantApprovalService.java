package com.erumpay.pg_auth_service.service;

import com.erumpay.pg_auth_service.client.MerchantServiceClient;
import com.erumpay.pg_auth_service.config.InternalApiProperties;
import com.erumpay.pg_auth_service.dto.MerchantApprovalResponse;
import com.erumpay.pg_auth_service.dto.MerchantServiceStatusResponse;
import com.erumpay.pg_auth_service.dto.MerchantStatusUpdateRequest;
import com.erumpay.pg_auth_service.entity.MerchantAccountStatus;
import com.erumpay.pg_auth_service.entity.MerchantAuth;
import com.erumpay.pg_auth_service.exception.AuthErrorCode;
import com.erumpay.pg_auth_service.exception.AuthException;
import com.erumpay.pg_auth_service.repository.MerchantAuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MerchantApprovalService {

	private final MerchantAuthRepository merchantAuthRepository;
	private final MerchantServiceClient merchantServiceClient;
	private final AdminAuditLogService adminAuditLogService;
	private final InternalApiProperties internalApiProperties;

	@Transactional
	public MerchantApprovalResponse approve(Long merchantId, Long adminId, String ipAddress) {
		MerchantAuth merchant = merchantAuthRepository.findByMerchantId(merchantId)
			.orElseThrow(() -> new AuthException(AuthErrorCode.MERCHANT_NOT_FOUND));

		if (merchant.getStatus() == MerchantAccountStatus.ACTIVE) {
			return new MerchantApprovalResponse(merchantId, merchant.getStatus());
		}
		if (merchant.getStatus() != MerchantAccountStatus.PENDING) {
			throw new AuthException(AuthErrorCode.MERCHANT_APPROVAL_NOT_ALLOWED);
		}

		MerchantServiceStatusResponse response;
		try {
			response = merchantServiceClient.updateMerchantStatus(
				internalApiProperties.apiKey(),
				merchantId,
				new MerchantStatusUpdateRequest(MerchantAccountStatus.ACTIVE)
			);
		} catch (RuntimeException exception) {
			throw new AuthException(AuthErrorCode.MERCHANT_STATUS_SYNC_FAILED, exception);
		}

		if (response == null
			|| !merchantId.equals(response.merchantId())
			|| response.status() != MerchantAccountStatus.ACTIVE) {
			throw new AuthException(AuthErrorCode.MERCHANT_STATUS_SYNC_FAILED);
		}

		merchant.setStatus(MerchantAccountStatus.ACTIVE);
		adminAuditLogService.record(
			adminId,
			"MERCHANT_APPROVED",
			String.valueOf(merchantId),
			"{\"status\":\"ACTIVE\"}",
			ipAddress
		);
		return new MerchantApprovalResponse(merchantId, merchant.getStatus());
	}
}
