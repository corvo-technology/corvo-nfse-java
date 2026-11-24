package technology.corvo.nfse.types;

public record SubstituteInvoice(
	String description,
	String notes,
	String purchaseOrder,
	SubstituteService service) {}
