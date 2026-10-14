package technology.corvo.nfse.types;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The {@code billing.plan} response. Nested field names are snake_case on the wire — mapped
 * with {@code @JsonProperty}; the wire format is passed through unchanged.
 */
public record BillingPlanResponse(Plan plan, Subscription subscription, Usage usage) {

	public record Plan(
			String slug,
			String name,
			@JsonProperty("monthly_allowance") Integer monthlyAllowance,
			@JsonProperty("daily_limit") Integer dailyLimit,
			@JsonProperty("max_active_companies") Integer maxActiveCompanies,
			@JsonProperty("monthly_price_cents") Integer monthlyPriceCents) {}

	public record Subscription(
			String status,
			@JsonProperty("paid_until") String paidUntil,
			@JsonProperty("cancel_at_period_end") Boolean cancelAtPeriodEnd,
			@JsonProperty("scheduled_plan") String scheduledPlan,
			Boolean blocked) {}

	public record Usage(
			@JsonProperty("overage_units") Integer overageUnits,
			@JsonProperty("overage_amount_cents") Integer overageAmountCents,
			@JsonProperty("hard_limit") Integer hardLimit,
			@JsonProperty("credits_used") Integer creditsUsed,
			@JsonProperty("credits_reserved") Integer creditsReserved,
			@JsonProperty("credits_remaining") Integer creditsRemaining,
			@JsonProperty("monthly_allowance") Integer monthlyAllowance,
			@JsonProperty("daily_usage") Integer dailyUsage,
			@JsonProperty("daily_limit") Integer dailyLimit,
			@JsonProperty("active_companies") Integer activeCompanies,
			@JsonProperty("max_active_companies") Integer maxActiveCompanies,
			@JsonProperty("period_start") String periodStart,
			@JsonProperty("period_end") String periodEnd) {}
}
