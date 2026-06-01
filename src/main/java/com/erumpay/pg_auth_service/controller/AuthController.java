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
import org.springframework.web.bind.annotation.RequestHeader;

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

	//약관동의 메서드
	@PostMapping("/merchant/terms/agree")
	public MerchantTermsAgreeResponse agreeMerchantTerms(
		@RequestHeader("Authorization") String authorization,
		@RequestBody MerchantTermsAgreeRequest request,
		HttpServletRequest httpServletRequest
	) {
		// 신규 가맹점은 아직 정식 Access Token이 없기 때문에 signup_token을 Authorization 헤더로 전달합니다.
		// Service에서는 이 signup_token을 검증하고, 토큰 안의 accountId를 꺼내 약관 동의 정보를 저장합니다.
		return authService.agreeMerchantTerms(authorization, request, httpServletRequest.getRemoteAddr());
	}

	// 회원가입 메서드
	@PostMapping("/merchant/signup")
	public MerchantSignupResponse signupMerchant(
		@RequestHeader("Authorization") String authorization,
		@RequestBody MerchantSignupRequest request
	) {
		// 회원가입 추가정보 입력도 signup_token을 통해 "어떤 가맹점 계정의 가입 절차인지" 확인합니다.
		// body로 accountId를 받으면 다른 계정 ID를 임의로 넣을 수 있으므로 토큰 기반으로 처리합니다.
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
