package technology.corvo.nfse.types;

/**
 * A purchasable credit plan. Serialized on the wire as {@code { data: { props: {...} }}} — the
 * SDK unwraps it transparently.
 */
public record CreditPlan(
		String id,
		String slug,
		String name,
		String description,
		Integer dailyLimit,
		Integer maxActiveCompanies,
		Integer creditUnitPriceCents,
		Double throughputRps,
		Boolean includesWebhooks,
		Boolean includesXmlDownload,
		Boolean includesPdfDownload,
		Boolean includesApiKeys,
		String supportTier,
		Boolean active,
		String createdAt,
		String updatedAt) {}
