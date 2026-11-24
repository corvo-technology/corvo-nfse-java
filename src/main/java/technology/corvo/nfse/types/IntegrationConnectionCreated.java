package technology.corvo.nfse.types;

import java.util.Map;

public record IntegrationConnectionCreated(
	String activeFrom,
	String companyId,
	String createdAt,
	String environment,
	String externalAccountId,
	String id,
	String provider,
	String state,
	String webhookUrl,
	Map<String, String> secrets) {}
