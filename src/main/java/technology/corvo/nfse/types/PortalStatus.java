package technology.corvo.nfse.types;

public record PortalStatus(
	Boolean available,
	String environment,
	String retryAt,
	String since) {}
