package technology.corvo.nfse.types;

import com.fasterxml.jackson.annotation.JsonProperty;

/** The plan recommended for a monthly volume. The wire names are snake_case. */
public record PlanRecommendation(
		Integer volume,
		@JsonProperty("recommended_plan") String recommendedPlan,
		@JsonProperty("plan_name") String planName,
		@JsonProperty("monthly_allowance") Integer monthlyAllowance,
		@JsonProperty("overage_credits") Integer overageCredits,
		@JsonProperty("overage_cost_cents") Integer overageCostCents,
		@JsonProperty("base_price_cents") Integer basePriceCents,
		@JsonProperty("total_estimated_cents") Integer totalEstimatedCents,
		String currency,
		@JsonProperty("billing_cycle") String billingCycle) {}
