package technology.corvo.nfse.types;

public record IntegrationEvent(
	Integer attempts,
	String createdAt,
	String deliveryId,
	String externalType,
	String id,
	String lastError,
	String operationId,
	String state) {}
