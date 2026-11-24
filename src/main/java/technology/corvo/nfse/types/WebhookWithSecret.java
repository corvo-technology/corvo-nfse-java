package technology.corvo.nfse.types;

import java.util.List;

public record WebhookWithSecret(
	Boolean active,
	String createdAt,
	String environment,
	List<String> events,
	String id,
	String updatedAt,
	String url,
	String secret) {}
