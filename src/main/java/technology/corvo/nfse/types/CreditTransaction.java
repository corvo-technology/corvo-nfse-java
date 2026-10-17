package technology.corvo.nfse.types;

/**
 * A credit ledger transaction. {@code amount} is in credits (not BRL); fields ending in {@code
 * Cents} are in centavos. {@code source} is {@code subscription}, {@code avulso} or {@code bonus}.
 */
public record CreditTransaction(
		String id,
		String tenantId,
		String type,
		Double amount,
		String description,
		String source,
		Integer amountBrlCents,
		Integer unitPriceCentsApplied,
		String planIdSnapshot,
		String reservedInvoiceId,
		String periodId,
		Boolean isReservation,
		String relatedTransactionId,
		String createdAt) {}
