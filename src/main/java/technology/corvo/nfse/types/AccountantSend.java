package technology.corvo.nfse.types;

public record AccountantSend(
	String createdAt,
	String id,
	Integer invoiceCount,
	String month,
	String recipient,
	String status,
	Integer totalCents,
	String trigger) {}
