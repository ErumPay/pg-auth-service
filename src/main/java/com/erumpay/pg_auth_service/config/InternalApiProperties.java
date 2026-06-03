package com.erumpay.pg_auth_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal")
public record InternalApiProperties(
	String apiKey
) {
}
