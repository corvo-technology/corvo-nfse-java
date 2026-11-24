package technology.corvo.nfse.types;

public record WebhookTestResult(
	Boolean sent,
	String error,
	Integer statusCode) {}
