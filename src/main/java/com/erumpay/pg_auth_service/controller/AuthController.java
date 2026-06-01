package com.erumpay.pg_auth_service.controller;

import com.erumpay.pg_auth_service.dto.AuthStatusResponse;
import com.erumpay.pg_auth_service.dto.KakaoMerchantLoginRequest;
import com.erumpay.pg_auth_service.dto.KakaoMerchantLoginResponse;
import com.erumpay.pg_auth_service.dto.LogoutRequest;
import com.erumpay.pg_auth_service.dto.MerchantSignupRequest;
import com.erumpay.pg_auth_service.dto.MerchantSignupResponse;
import com.erumpay.pg_auth_service.dto.MerchantTermsAgreeRequest;
import com.erumpay.pg_auth_service.dto.MerchantTermsAgreeResponse;
import com.erumpay.pg_auth_service.dto.TokenRefreshRequest;
import com.erumpay.pg_auth_service.dto.TokenRefreshResponse;
import com.erumpay.pg_auth_service.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	@GetMapping("/health")
	public AuthStatusResponse health() {
		return authService.getStatus();
	}

	// 가맹점 관련 API
	@PostMapping("/merchant/kakao/login")
	public KakaoMerchantLoginResponse loginMerchantWithKakao(
		@RequestBody KakaoMerchantLoginRequest request
	) {
		return authService.loginMerchantWithKakao(request);
	}

	@PostMapping("/merchant/terms/agree")
	public MerchantTermsAgreeResponse agreeMerchantTerms(
		@RequestBody MerchantTermsAgreeRequest request,
		HttpServletRequest httpServletRequest
	) {
		return authService.agreeMerchantTerms(request, httpServletRequest.getRemoteAddr());
	}

	@PostMapping("/merchant/signup")
	public MerchantSignupResponse signupMerchant(@RequestBody MerchantSignupRequest request) {
		return authService.signupMerchant(request);
	}

	@PostMapping("/token/refresh")
	public TokenRefreshResponse refreshAccessToken(@RequestBody TokenRefreshRequest request) {
		return authService.refreshAccessToken(request);
	}

	@PostMapping("/merchant/logout")
	public void logoutMerchant(@RequestBody LogoutRequest request) {
		authService.logoutMerchant(request);
	}
}
