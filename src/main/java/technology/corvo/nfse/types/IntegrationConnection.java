package technology.corvo.nfse.types;

public record IntegrationConnection(
	String activeFrom,
	String companyId,
	String createdAt,
	String environment,
	String externalAccountId,
	String id,
	String provider,
	String state,
	String webhookUrl) {}
