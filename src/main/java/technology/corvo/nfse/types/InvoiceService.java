package technology.corvo.nfse.types;

public record InvoiceService(
	String code,
	String description,
	ServiceFiscal fiscal,
	String municipalityCode,
	String serviceProfileId) {}
