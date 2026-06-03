package com.erumpay.pg_auth_service.security;

import com.erumpay.pg_auth_service.config.RedisConfig;
import com.erumpay.pg_auth_service.entity.MerchantAccountStatus;
import com.erumpay.pg_auth_service.repository.MerchantAuthRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Authorization: Bearer ... 헤더를 읽어 Spring Security 인증 객체로 바꿉니다.
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final StringRedisTemplate redisTemplate;
	private final MerchantAuthRepository merchantAuthRepository;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String accessToken = extractBearerToken(request);

		if (accessToken != null
			&& jwtService.validateToken(accessToken)
			&& JwtTokenType.ACCESS.name().equals(jwtService.extractTokenType(accessToken))
			&& !isBlacklisted(accessToken)) {
			Long accountId = jwtService.extractAccountId(accessToken);
			String role = jwtService.extractRole(accessToken);
			if (JwtRole.MERCHANT.name().equals(role) && !isActiveMerchant(accountId)) {
				filterChain.doFilter(request, response);
				return;
			}
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				accountId,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_" + role))
			);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}

		filterChain.doFilter(request, response);
	}

	private String extractBearerToken(HttpServletRequest request) {
		String authorization = request.getHeader("Authorization");
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return null;
		}
		return authorization.substring(7);
	}

	private boolean isBlacklisted(String accessToken) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(RedisConfig.ACCESS_BLACKLIST_KEY_PREFIX + accessToken));
	}

	private boolean isActiveMerchant(Long accountId) {
		return merchantAuthRepository.findById(accountId)
			.map(merchant -> merchant.getStatus() == MerchantAccountStatus.ACTIVE)
			.orElse(false);
	}
}
