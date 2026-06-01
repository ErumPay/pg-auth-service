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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	@PostMapping("/merchant/kakao/login")
	public KakaoMerchantLoginResponse loginMerchantWithKakao(
		@RequestBody KakaoMerchantLoginRequest request
	) {
		return authService.loginMerchantWithKakao(request);
	}

	@GetMapping("/login/oauth2/code/kakao")
	public KakaoMerchantLoginResponse kakaoCallback(@RequestParam String code) {
		// 프론트가 없는 현재 단계에서 카카오가 redirect_uri로 보내주는 인가 코드(code)를 직접 받습니다.
		// 이후 처리는 POST /merchant/kakao/login과 같은 서비스 로직을 재사용합니다.
		return authService.loginMerchantWithKakao(new KakaoMerchantLoginRequest(code));
	}

	@PostMapping("/merchant/terms/agree")
	public MerchantTermsAgreeResponse agreeMerchantTerms(
		@RequestHeader("Authorization") String authorization,
		@RequestBody MerchantTermsAgreeRequest request,
		HttpServletRequest httpServletRequest
	) {
		// 신규 가맹점은 아직 Access Token이 없으므로 signup_token으로 가입 절차의 계정을 식별합니다.
		return authService.agreeMerchantTerms(authorization, request, httpServletRequest.getRemoteAddr());
	}

	@PostMapping("/merchant/signup")
	public MerchantSignupResponse signupMerchant(
		@RequestHeader("Authorization") String authorization,
		@RequestBody MerchantSignupRequest request
	) {
		// 사업자/정산/서비스 정보를 받은 뒤 merchant-service에 가맹점 생성을 요청합니다.
		return authService.signupMerchant(authorization, request);
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
