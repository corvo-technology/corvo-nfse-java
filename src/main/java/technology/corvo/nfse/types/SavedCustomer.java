package technology.corvo.nfse.types;

public record SavedCustomer(
	String createdAt,
	String email,
	String lastUsedAt,
	String name,
	String taxId,
	String updatedAt) {}
