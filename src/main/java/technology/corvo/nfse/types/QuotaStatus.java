package technology.corvo.nfse.types;

/** The {@code billing.usage} response: current emission quota. */
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
		String periodEnd) {}
