package technology.corvo.nfse.types;

public record FiscalProfile(
	String companyId,
	String createdAt,
	String ibsCbsRegularFrom,
	Boolean ibsCbsRegularOption,
	String id,
	String reviewedAt,
	String reviewedBy,
	String reviewStatus,
	Boolean simpleNational,
	String simplesApuracao,
	String simplesIssRate,
	String source,
	String specialTaxRegime,
	String taxRegime,
	String tenantId,
	String validFrom,
	String validUntil) {}
