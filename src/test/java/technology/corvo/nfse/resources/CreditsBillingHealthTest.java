package technology.corvo.nfse.resources;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import technology.corvo.nfse.Corvo;
import technology.corvo.nfse.TestHarness;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.types.BillingPlanResponse;
import technology.corvo.nfse.types.CreditBalance;
import technology.corvo.nfse.types.CreditPlan;
import technology.corvo.nfse.types.CreditTransaction;
import technology.corvo.nfse.types.Paginated;
import technology.corvo.nfse.types.QuotaStatus;

class CreditsBillingHealthTest extends TestHarness {

	@Test
	void creditBalance() throws Exception {
		enqueueJson(200,
				"{\"data\":{\"tenantId\":\"t_1\",\"balance\":100,\"overageUnits\":0,"
						+ "\"overageAmountCents\":0,\"hardLimit\":null}}");

		Result<CreditBalance> result = corvo().credits().balance().get(5, TimeUnit.SECONDS);

		assertThat(((Result.Ok<CreditBalance>) result).data().balance()).isEqualTo(100.0);
		assertThat(takeRequest().getPath()).isEqualTo("/v1/credits");
	}

	@Test
	void creditPlansUnwrapProps() throws Exception {
		enqueueJson(
				200,
				"{\"data\":[{\"id\":\"plan_1\",\"props\":{\"id\":\"plan_1\",\"slug\":\"starter\","
						+ "\"name\":\"Starter\",\"description\":\"\",\"dailyLimit\":100,"
						+ "\"maxActiveCompanies\":1,\"creditUnitPriceCents\":100,"
						+ "\"throughputRps\":1.0,\"includesWebhooks\":true,\"includesXmlDownload\":true,"
						+ "\"includesPdfDownload\":true,\"includesApiKeys\":true,\"supportTier\":\"community\","
						+ "\"active\":true,\"createdAt\":\"2026-10-01T00:00:00Z\","
						+ "\"updatedAt\":\"2026-10-01T00:00:00Z\"}}],"
						+ "\"pagination\":{\"limit\":50,\"hasMore\":false,\"nextCursor\":null}}");

		Result<Paginated<CreditPlan>> result =
				corvo().credits().plans(null, null).get(5, TimeUnit.SECONDS);

		assertThat(((Result.Ok<Paginated<CreditPlan>>) result).data().data().get(0).slug())
				.isEqualTo("starter");
	}

	@Test
	void creditHistoryAndIterate() throws Exception {
		enqueueJson(
				200,
				"{\"data\":[{\"id\":\"tx_1\",\"props\":{\"id\":\"tx_1\",\"type\":\"invoice\","
						+ "\"amount\":1.0,\"creditDelta\":-1.0,\"balanceAfter\":99.0,"
						+ "\"environment\":\"test\",\"source\":\"api\",\"referenceType\":\"invoice\","
						+ "\"referenceId\":\"inv_1\",\"createdAt\":\"2026-10-01T00:00:00Z\"}}],"
						+ "\"pagination\":{\"limit\":50,\"hasMore\":false,\"nextCursor\":null}}");
		enqueueJson(
				200,
				"{\"data\":[{\"id\":\"tx_1\",\"props\":{\"id\":\"tx_1\",\"type\":\"invoice\","
						+ "\"amount\":1.0,\"creditDelta\":-1.0,\"balanceAfter\":99.0,"
						+ "\"environment\":\"test\",\"source\":\"api\",\"referenceType\":\"invoice\","
						+ "\"referenceId\":\"inv_1\",\"createdAt\":\"2026-10-01T00:00:00Z\"}}],"
						+ "\"pagination\":{\"limit\":50,\"hasMore\":false,\"nextCursor\":null}}");

		Result<Paginated<CreditTransaction>> page =
				corvo().credits().history(50, null).get(5, TimeUnit.SECONDS);
		assertThat(((Result.Ok<Paginated<CreditTransaction>>) page).data().data().get(0).creditDelta())
				.isEqualTo(-1.0);

		List<CreditTransaction> all = corvo().credits().iterateHistory(50, null).toList();
		assertThat(all).hasSize(1);
	}

	@Test
	void billingPlanMapsSnakeCaseWireFields() throws Exception {
		enqueueJson(
				200,
				"{\"data\":{\"plan\":{\"slug\":\"pro\",\"name\":\"Pro\",\"monthly_allowance\":500,"
						+ "\"daily_limit\":50,\"max_active_companies\":5,\"monthly_price_cents\":9900},"
						+ "\"subscription\":{\"status\":\"active\",\"paid_until\":\"2026-11-01\","
						+ "\"cancel_at_period_end\":false,\"scheduled_plan\":null,\"blocked\":false},"
						+ "\"usage\":{\"overage_units\":2,\"overage_amount_cents\":200,\"hard_limit\":null,"
						+ "\"credits_used\":498,\"credits_reserved\":0,\"credits_remaining\":2,"
						+ "\"monthly_allowance\":500,\"daily_usage\":10,\"daily_limit\":50,"
						+ "\"active_companies\":3,\"max_active_companies\":5,"
						+ "\"period_start\":\"2026-10-01\",\"period_end\":\"2026-10-31\"}}}");

		Result<BillingPlanResponse> result = corvo().billing().plan().get(5, TimeUnit.SECONDS);

		BillingPlanResponse plan = ((Result.Ok<BillingPlanResponse>) result).data();
		assertThat(plan.plan().monthlyAllowance()).isEqualTo(500);
		assertThat(plan.subscription().cancelAtPeriodEnd()).isFalse();
		assertThat(plan.usage().creditsRemaining()).isEqualTo(2);
		assertThat(plan.usage().periodEnd()).isEqualTo("2026-10-31");
	}

	@Test
	void billingUsage() throws Exception {
		enqueueJson(
				200,
				"{\"data\":{\"canEmit\":true,\"creditsRemaining\":42,\"creditsUsed\":8,"
						+ "\"creditsReserved\":0,\"monthlyAllowance\":500,\"dailyUsage\":3,"
						+ "\"dailyLimit\":50,\"activeCompanies\":2,\"maxActiveCompanies\":5,"
						+ "\"periodStart\":\"2026-10-01\",\"periodEnd\":\"2026-10-31\"}}");

		Result<QuotaStatus> result = corvo().billing().usage().get(5, TimeUnit.SECONDS);

		assertThat(((Result.Ok<QuotaStatus>) result).data().canEmit()).isTrue();
	}

	@Test
	void healthLiveAndReadyReturnRawJson() throws Exception {
		enqueueJson(200, "{\"status\":\"ok\"}");
		enqueueJson(200, "{\"status\":\"ok\",\"database\":\"ok\"}");

		com.fasterxml.jackson.databind.JsonNode live =
				((Result.Ok<com.fasterxml.jackson.databind.JsonNode>) corvo().health()
								.live()
								.get(5, TimeUnit.SECONDS))
						.data();
		assertThat(live.get("status").asText()).isEqualTo("ok");
		// unwrap:none — whole body returned, not {data:...}
		assertThat(live.has("data")).isFalse();

		com.fasterxml.jackson.databind.JsonNode ready =
				((Result.Ok<com.fasterxml.jackson.databind.JsonNode>) corvo().health()
								.ready()
								.get(5, TimeUnit.SECONDS))
						.data();
		assertThat(ready.get("database").asText()).isEqualTo("ok");
	}

	private Corvo corvo() {
		return Corvo.builder().apiKey(API_KEY).baseUrl(server.url("/").toString()).build();
	}
}
