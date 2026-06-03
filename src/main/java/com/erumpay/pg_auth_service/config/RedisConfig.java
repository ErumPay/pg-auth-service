package com.erumpay.pg_auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

// Redis에는 Refresh Token과 Access Token blacklist를 TTL과 함께 저장합니다.
@Configuration
public class RedisConfig {

	public static final String MERCHANT_REFRESH_KEY_PREFIX = "refresh:merchant:";
	public static final String ACCESS_BLACKLIST_KEY_PREFIX = "blacklist:access:";

	@Bean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		return new StringRedisTemplate(redisConnectionFactory);
	}
}
