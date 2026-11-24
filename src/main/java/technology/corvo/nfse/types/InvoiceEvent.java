package technology.corvo.nfse.types;

public record InvoiceEvent(
	String createdAt,
	String fromStatus,
	String id,
	String invoiceId,
	String portalEvent,
	String portalNsu,
	String reason,
	String source,
	String toStatus) {}
