package technology.corvo.nfse.types;

public record PackageSummary(
	String companyName,
	String companyTaxId,
	Integer count,
	Integer limit,
	String month,
	String total) {}
