package technology.corvo.nfse.types;

/**
 * A paid plan of the public price list, in centavos. {@code monthlyEquivalentCents} is the price
 * per month in the chosen cycle (the annual cycle is discounted); {@code chargeTotalCents} is what
 * is charged at once for that cycle.
 */
public record PlanPrice(
		String slug,
		String name,
		Integer monthlyAllowance,
		Integer basePriceCents,
		Integer monthlyEquivalentCents,
		Integer chargeTotalCents,
		String currency) {}
