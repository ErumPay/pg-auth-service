package com.erumpay.pg_auth_service.security;

import com.erumpay.pg_auth_service.config.InternalApiProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class InternalApiAuthenticationFilter extends OncePerRequestFilter {

	private static final String INTERNAL_PATH_PREFIX = "/internal/v1/auth/";
	private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

	private final InternalApiProperties internalApiProperties;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		if (request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX)
			&& isValidInternalApiKey(request.getHeader(INTERNAL_API_KEY_HEADER))) {
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				"internal-service",
				null,
				List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))
			);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}

		filterChain.doFilter(request, response);
	}

	private boolean isValidInternalApiKey(String apiKey) {
		String configuredApiKey = internalApiProperties.apiKey();
		return configuredApiKey != null
			&& !configuredApiKey.isBlank()
			&& apiKey != null
			&& MessageDigest.isEqual(
				configuredApiKey.getBytes(StandardCharsets.UTF_8),
				apiKey.getBytes(StandardCharsets.UTF_8)
			);
	}
}
