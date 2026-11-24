package technology.corvo.nfse.types;

public record IntegrationOperation(
	Integer amountCents,
	String blockReason,
	String commercialState,
	String createdAt,
	String currency,
	Customer customer,
	String fiscalStatus,
	String id,
	String invoiceId,
	String operationKey,
	String paidAt,
	String updatedAt) {

	public record Customer(
		String email,
		String name,
		String taxId,
		Boolean taxIdMasked) {}
}
