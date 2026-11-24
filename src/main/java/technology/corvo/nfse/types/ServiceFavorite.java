package technology.corvo.nfse.types;

public record ServiceFavorite(
	Integer amountCents,
	String companyId,
	String createdAt,
	String description,
	String id,
	String label,
	String municipalityCode,
	String serviceCode,
	String tenantId) {}
