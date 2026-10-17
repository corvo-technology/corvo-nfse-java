package technology.corvo.nfse.types;

/** An entry of the public price catalog: prices in centavos, monthly cycle only. */
public record PlanQuote(
		String slug,
		String name,
		Integer monthlyAllowance,
		Integer basePriceCents,
		Integer perCreditOverageCents,
		Integer monthlyTotalCents,
		String currency) {}
