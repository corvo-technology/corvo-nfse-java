package technology.corvo.nfse.types;

public record IntegrationRuleInput(
	Boolean allowAnonymousCustomer,
	Boolean cancelOnRefund,
	String externalProductId,
	Boolean sendEmail,
	InvoiceService service) {}
