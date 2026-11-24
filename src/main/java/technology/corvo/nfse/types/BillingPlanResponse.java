package technology.corvo.nfse.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BillingPlanResponse(
	Plan plan,
	Subscription subscription,
	Usage usage) {

	public record Plan(
		@JsonProperty("daily_limit") Integer dailyLimit,
		@JsonProperty("max_active_companies") Integer maxActiveCompanies,
		@JsonProperty("monthly_allowance") Integer monthlyAllowance,
		@JsonProperty("monthly_price_cents") Integer monthlyPriceCents,
		String name,
		String slug) {}

	public record Subscription(
		Boolean blocked,
		@JsonProperty("cancel_at_period_end") Boolean cancelAtPeriodEnd,
		@JsonProperty("paid_until") String paidUntil,
		@JsonProperty("pix_subscription") PixSubscription pixSubscription,
		@JsonProperty("scheduled_plan") String scheduledPlan,
		String status,
		@JsonProperty("stripe_portal_available") Boolean stripePortalAvailable,
		@JsonProperty("stripe_subscription") Boolean stripeSubscription) {

		public record PixSubscription(
			String brCode,
			String paymentId,
			String paymentLinkUrl,
			String planSlug,
			String status) {}
	}

	public record Usage(
		@JsonProperty("active_companies") Integer activeCompanies,
		@JsonProperty("avulso_balance") Integer avulsoBalance,
		@JsonProperty("credits_remaining") Integer creditsRemaining,
		@JsonProperty("credits_reserved") Integer creditsReserved,
		@JsonProperty("credits_used") Integer creditsUsed,
		@JsonProperty("daily_limit") Integer dailyLimit,
		@JsonProperty("daily_usage") Integer dailyUsage,
		@JsonProperty("hard_limit") Integer hardLimit,
		@JsonProperty("max_active_companies") Integer maxActiveCompanies,
		@JsonProperty("monthly_allowance") Integer monthlyAllowance,
		@JsonProperty("period_end") String periodEnd,
		@JsonProperty("period_start") String periodStart) {}
}
