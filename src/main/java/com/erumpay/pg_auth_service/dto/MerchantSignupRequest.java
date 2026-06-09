package com.erumpay.pg_auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MerchantSignupRequest(
	@JsonProperty("business_number")
	String businessNumber,

	@JsonProperty("merchant_name")
	String merchantName,

	@JsonProperty("mcc_code")
	String mccCode,

	@JsonProperty("representative_name")
	String representativeName,

	@JsonProperty("contact_email")
	String contactEmail,

	@JsonProperty("contact_phone")
	String contactPhone,

	@JsonProperty("business_address")
	String businessAddress,

	@JsonProperty("settlement_account")
	String settlementAccount,

	@JsonProperty("bank_name")
	String bankName,

	@JsonProperty("service_name")
	String serviceName
) {
}
