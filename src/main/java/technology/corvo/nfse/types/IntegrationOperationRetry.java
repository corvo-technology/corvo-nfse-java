package technology.corvo.nfse.types;

public record IntegrationOperationRetry(
	String lastError,
	String operationId,
	String state) {}
