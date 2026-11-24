package technology.corvo.nfse.types;

import java.util.Map;

public record CreateConnection(
	String activeFrom,
	String companyId,
	Map<String, String> credentials,
	String environment,
	String provider) {}
