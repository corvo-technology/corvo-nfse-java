package technology.corvo.nfse.types;

/** Result of sending an invoice by email. */
public record InvoiceEmailResult(String emailId, String status) {}
