package technology.corvo.nfse.types;

/**
 * Empty text means the registry did not say.
 */
public record CnpjProfile(
	Boolean active,
	PostalAddress address,
	String legalName,
	Boolean mei,
	String municipalityCode,
	Boolean simples,
	String situation,
	String tradeName) {}
