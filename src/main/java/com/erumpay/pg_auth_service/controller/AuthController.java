package com.erumpay.pg_auth_service.controller;

import com.erumpay.pg_auth_service.dto.AuthStatusResponse;
import com.erumpay.pg_auth_service.dto.KakaoMerchantLoginRequest;
import com.erumpay.pg_auth_service.dto.KakaoMerchantLoginResponse;
import com.erumpay.pg_auth_service.dto.LogoutRequest;
import com.erumpay.pg_auth_service.dto.MerchantSignupRequest;
import com.erumpay.pg_auth_service.dto.MerchantSignupResponse;
import com.erumpay.pg_auth_service.dto.MerchantStatusUpdateRequest;
import com.erumpay.pg_auth_service.dto.MerchantStatusUpdateResponse;
import com.erumpay.pg_auth_service.dto.MerchantTermsAgreeRequest;
import com.erumpay.pg_auth_service.dto.MerchantTermsAgreeResponse;
import com.erumpay.pg_auth_service.dto.MerchantTokenRevokeResponse;
import com.erumpay.pg_auth_service.dto.MessageResponse;
import com.erumpay.pg_auth_service.dto.TokenRefreshRequest;
import com.erumpay.pg_auth_service.dto.TokenRefreshResponse;
import com.erumpay.pg_auth_service.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class AuthController {

	private final AuthService authService;

	@GetMapping("/api/v1/auth/health")
	public AuthStatusResponse health() {
		return authService.getStatus();
	}

	@PostMapping("/api/v1/auth/merchant/kakao/login")
	public KakaoMerchantLoginResponse loginMerchantWithKakao(@RequestBody KakaoMerchantLoginRequest request) {
		return authService.loginMerchantWithKakao(request);
	}

	@GetMapping("/api/v1/auth/login/oauth2/code/kakao")
	public KakaoMerchantLoginResponse kakaoCallback(@RequestParam String code) {
		return authService.loginMerchantWithKakao(new KakaoMerchantLoginRequest(code));
	}

	@PostMapping("/api/v1/auth/merchant/terms/agree")
	public MerchantTermsAgreeResponse agreeMerchantTerms(
		@RequestHeader("Authorization") String authorization,
		@RequestBody MerchantTermsAgreeRequest request,
		HttpServletRequest httpServletRequest
	) {
		return authService.agreeMerchantTerms(authorization, request, httpServletRequest.getRemoteAddr());
	}

	@PostMapping("/api/v1/auth/merchant/signup")
	public MerchantSignupResponse signupMerchant(
		@RequestHeader("Authorization") String authorization,
		@RequestBody MerchantSignupRequest request
	) {
		return authService.signupMerchant(authorization, request);
	}

	@PostMapping("/api/v1/auth/token/refresh")
	public TokenRefreshResponse refreshAccessToken(
		@RequestHeader(value = "Authorization", required = false) String authorization,
		@RequestBody(required = false) TokenRefreshRequest request
	) {
		return authService.refreshAccessToken(authorization, request);
	}

	@PostMapping("/api/v1/auth/merchant/logout")
	public MessageResponse logoutMerchant(
		@RequestHeader(value = "Authorization", required = false) String authorization,
		@RequestBody(required = false) LogoutRequest request
	) {
		authService.logoutMerchant(authorization, request);
		return new MessageResponse("로그아웃 완료");
	}

	@PatchMapping("/internal/v1/auth/merchants/{merchantId}/tokens/revoke")
	public MerchantTokenRevokeResponse revokeMerchantTokens(@PathVariable Long merchantId) {
		return authService.revokeMerchantTokens(merchantId);
	}

	@PatchMapping("/internal/v1/auth/merchants/{merchantId}/status")
	public MerchantStatusUpdateResponse updateMerchantStatus(
		@PathVariable Long merchantId,
		@RequestBody MerchantStatusUpdateRequest request
	) {
		return authService.updateMerchantStatus(merchantId, request);
	}
}
