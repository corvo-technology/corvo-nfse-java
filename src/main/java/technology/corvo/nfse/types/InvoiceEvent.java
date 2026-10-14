package technology.corvo.nfse.types;

/** A status-transition event in an invoice's lifecycle. */
public record InvoiceEvent(
		String id, String invoiceId, String fromStatus, String toStatus, String reason, String createdAt) {}
