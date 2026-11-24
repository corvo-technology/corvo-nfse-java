package technology.corvo.nfse.types;

import java.util.Map;

public record UpdateCredentials(
	Map<String, String> credentials) {}
