package com.erumpay.pg_auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PgAuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PgAuthServiceApplication.class, args);
	}

}
