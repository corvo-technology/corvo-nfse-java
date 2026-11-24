package technology.corvo.nfse.types;

import java.util.Map;

public record ServiceSnapshot(
	String code,
	String description,
	Map<String, String> fiscal,
	String municipalityCode,
	String notes,
	String purchaseOrder,
	String serviceProfileId) {}
