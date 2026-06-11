package com.erumpay.pg_auth_service.config;

import com.erumpay.pg_auth_service.client.KakaoProperties;
import com.erumpay.pg_auth_service.security.InternalApiAuthenticationFilter;
import com.erumpay.pg_auth_service.security.JwtAuthenticationFilter;
import com.erumpay.pg_auth_service.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties({ JwtProperties.class, KakaoProperties.class, InternalApiProperties.class })
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final InternalApiAuthenticationFilter internalApiAuthenticationFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.cors(Customizer.withDefaults())
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.formLogin(form -> form.disable())
			.httpBasic(basic -> basic.disable())
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/internal/v1/auth/**").hasRole("INTERNAL_SERVICE")
				.requestMatchers("/api/v1/pg-admin/**").hasRole("PG_ADMIN")
				.requestMatchers("/api/v1/auth/**").permitAll()
				.requestMatchers(
					"/actuator/health",
					"/actuator/health/**",
					"/v3/api-docs/**",
					"/swagger-ui/**",
					"/swagger-ui.html"
				).permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(internalApiAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
