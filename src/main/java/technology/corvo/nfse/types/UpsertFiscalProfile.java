package technology.corvo.nfse.types;

public record UpsertFiscalProfile(
	Boolean confirm,
	String ibsCbsRegularFrom,
	Boolean ibsCbsRegularOption,
	Boolean simpleNational,
	String simplesApuracao,
	String specialTaxRegime,
	String taxRegime,
	String validFrom) {}
