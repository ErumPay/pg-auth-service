package com.erumpay.pg_auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:pg_auth_service_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
	"jwt.secret=test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256",
	"kakao.client-id=test-kakao-client-id",
	"internal.api-key=test-internal-api-key"
})
class PgAuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}
}
