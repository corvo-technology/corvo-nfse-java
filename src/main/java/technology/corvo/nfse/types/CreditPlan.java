package technology.corvo.nfse.types;

/**
 * A plan with its limits and features. {@code dailyLimit} is {@code null} when the plan has no
 * daily limit.
 */
public record CreditPlan(
		String id,
		String slug,
		String name,
		String description,
		Integer monthlyCreditAllowance,
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
