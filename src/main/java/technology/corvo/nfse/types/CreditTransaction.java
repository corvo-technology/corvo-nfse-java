package technology.corvo.nfse.types;

/**
 * A credit ledger transaction. Serialized on the wire as {@code { data: { props: {...} }}} — the
 * SDK unwraps it transparently.
 */
public record CreditTransaction(
		String id,
		String type,
		Double amount,
		Double creditDelta,
		Double balanceAfter,
		String environment,
		String source,
		String referenceType,
		String referenceId,
		String createdAt) {}
