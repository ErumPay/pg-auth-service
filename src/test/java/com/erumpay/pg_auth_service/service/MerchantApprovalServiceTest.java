package com.erumpay.pg_auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.erumpay.pg_auth_service.client.MerchantServiceClient;
import com.erumpay.pg_auth_service.config.InternalApiProperties;
import com.erumpay.pg_auth_service.dto.MerchantApprovalResponse;
import com.erumpay.pg_auth_service.dto.MerchantServiceStatusResponse;
import com.erumpay.pg_auth_service.dto.MerchantStatusUpdateRequest;
import com.erumpay.pg_auth_service.entity.MerchantAccountRole;
import com.erumpay.pg_auth_service.entity.MerchantAccountStatus;
import com.erumpay.pg_auth_service.entity.MerchantAuth;
import com.erumpay.pg_auth_service.exception.AuthErrorCode;
import com.erumpay.pg_auth_service.exception.AuthException;
import com.erumpay.pg_auth_service.repository.MerchantAuthRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MerchantApprovalServiceTest {

	@Mock
	private MerchantAuthRepository merchantAuthRepository;

	@Mock
	private MerchantServiceClient merchantServiceClient;

	@Mock
	private AdminAuditLogService adminAuditLogService;

	private MerchantApprovalService merchantApprovalService;

	@BeforeEach
	void setUp() {
		merchantApprovalService = new MerchantApprovalService(
			merchantAuthRepository,
			merchantServiceClient,
			adminAuditLogService,
			new InternalApiProperties("test-internal-key")
		);
	}

	@Test
	void approveChangesBothMerchantStatusesToActive() {
		MerchantAuth merchant = merchant(10L, MerchantAccountStatus.PENDING);
		when(merchantAuthRepository.findByMerchantId(10L)).thenReturn(Optional.of(merchant));
		when(merchantServiceClient.updateMerchantStatus(
			"test-internal-key",
			10L,
			new MerchantStatusUpdateRequest(MerchantAccountStatus.ACTIVE)
		)).thenReturn(new MerchantServiceStatusResponse(10L, MerchantAccountStatus.ACTIVE));

		MerchantApprovalResponse response =
			merchantApprovalService.approve(10L, 1L, "127.0.0.1");

		assertThat(response.status()).isEqualTo(MerchantAccountStatus.ACTIVE);
		assertThat(merchant.getStatus()).isEqualTo(MerchantAccountStatus.ACTIVE);
	}

	@Test
	void approveIsIdempotentForActiveMerchant() {
		MerchantAuth merchant = merchant(10L, MerchantAccountStatus.ACTIVE);
		when(merchantAuthRepository.findByMerchantId(10L)).thenReturn(Optional.of(merchant));

		MerchantApprovalResponse response =
			merchantApprovalService.approve(10L, 1L, "127.0.0.1");

		assertThat(response.status()).isEqualTo(MerchantAccountStatus.ACTIVE);
		verify(merchantServiceClient, never()).updateMerchantStatus(
			"test-internal-key",
			10L,
			new MerchantStatusUpdateRequest(MerchantAccountStatus.ACTIVE)
		);
	}

	@Test
	void approveRejectsNonPendingMerchant() {
		MerchantAuth merchant = merchant(10L, MerchantAccountStatus.DRAFT);
		when(merchantAuthRepository.findByMerchantId(10L)).thenReturn(Optional.of(merchant));

		assertThatThrownBy(() ->
			merchantApprovalService.approve(10L, 1L, "127.0.0.1"))
			.isInstanceOfSatisfying(AuthException.class, exception ->
				assertThat(exception.getErrorCode())
					.isEqualTo(AuthErrorCode.MERCHANT_APPROVAL_NOT_ALLOWED)
			);
	}

	@Test
	void approveDoesNotChangeAuthStatusWhenMerchantServiceFails() {
		MerchantAuth merchant = merchant(10L, MerchantAccountStatus.PENDING);
		when(merchantAuthRepository.findByMerchantId(10L)).thenReturn(Optional.of(merchant));
		when(merchantServiceClient.updateMerchantStatus(
			"test-internal-key",
			10L,
			new MerchantStatusUpdateRequest(MerchantAccountStatus.ACTIVE)
		)).thenThrow(new IllegalStateException("merchant-service unavailable"));

		assertThatThrownBy(() ->
			merchantApprovalService.approve(10L, 1L, "127.0.0.1"))
			.isInstanceOfSatisfying(AuthException.class, exception ->
				assertThat(exception.getErrorCode())
					.isEqualTo(AuthErrorCode.MERCHANT_STATUS_SYNC_FAILED)
			);
		assertThat(merchant.getStatus()).isEqualTo(MerchantAccountStatus.PENDING);
	}

	private MerchantAuth merchant(Long merchantId, MerchantAccountStatus status) {
		MerchantAuth merchant = new MerchantAuth();
		merchant.setMerchantId(merchantId);
		merchant.setKakaoOauthId("kakao-1");
		merchant.setName("테스트 가맹점");
		merchant.setRole(MerchantAccountRole.OWNER);
		merchant.setStatus(status);
		return merchant;
	}
}
