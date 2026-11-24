package technology.corvo.nfse.types;

public record CreateServiceFavorite(
	Double amountCents,
	String description,
	String label,
	String municipalityCode,
	String serviceCode) {}
