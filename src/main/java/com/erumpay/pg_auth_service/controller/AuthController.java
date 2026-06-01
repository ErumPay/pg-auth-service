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
import org.springframework.web.bind.annotation.RequestParam;

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

	@GetMapping("/login/oauth2/code/kakao")
	public KakaoMerchantLoginResponse kakaoCallback(@RequestParam String code) {
		// 카카오 로그인 화면에서 "동의하고 시작하기"를 누르면,
		// 카카오가 redirect_uri로 사용자를 다시 보내면서 URL 뒤에 code를 붙여줍니다.
		// 예: /api/v1/auth/login/oauth2/code/kakao?code=abc123
		//
		// 이 code는 카카오 access token을 발급받기 위한 1회용 인가 코드입니다.
		// 기존 POST /merchant/kakao/login 로직이 code를 받아 처리하도록 만들어져 있으므로,
		// 여기서는 request DTO로 감싸서 같은 서비스 로직을 재사용합니다.
		return authService.loginMerchantWithKakao(new KakaoMerchantLoginRequest(code));
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
