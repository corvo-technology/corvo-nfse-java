package technology.corvo.nfse.types;

/** The tenant's credit balance. */
public record CreditBalance(
		String tenantId, Double balance, Integer overageUnits, Integer overageAmountCents, Double hardLimit) {}
