package technology.corvo.nfse.types;

import java.util.List;

public record Obligation(
	List<String> missingFields,
	String reason,
	String status) {}
