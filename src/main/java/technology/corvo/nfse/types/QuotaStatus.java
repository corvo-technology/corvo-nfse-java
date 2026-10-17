package technology.corvo.nfse.types;

/**
 * The {@code billing.usage} response: current emission quota. {@code reason} is set when {@code
 * canEmit} is false: no_credits, daily_limit_exceeded, company_limit_exceeded or plan_blocked.
 */
public record QuotaStatus(
		Boolean canEmit,
		Integer creditsRemaining,
		Integer creditsUsed,
		Integer creditsReserved,
		Integer monthlyAllowance,
		Integer dailyUsage,
		Integer dailyLimit,
		Integer activeCompanies,
		Integer maxActiveCompanies,
		String periodStart,
		String periodEnd,
		String reason) {}
