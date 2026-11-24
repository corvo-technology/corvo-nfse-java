package technology.corvo.nfse.types;

import java.util.Map;

public record CreditTransaction(
	Integer amount,
	Integer amountBrlCents,
	String createdAt,
	String description,
	String id,
	Boolean isReservation,
	Map<String, Object> metadata,
	String periodId,
	String planIdSnapshot,
	String relatedTransactionId,
	String reservedInvoiceId,
	String source,
	String tenantId,
	String type,
	Integer unitPriceCentsApplied) {}
