package technology.corvo.nfse.resources;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import technology.corvo.nfse.Corvo;
import technology.corvo.nfse.TestHarness;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.types.InvoiceCalculation;
import technology.corvo.nfse.types.InvoiceCreateParams;
import technology.corvo.nfse.types.Paginated;
import technology.corvo.nfse.types.PlanQuote;
import technology.corvo.nfse.types.PlanRecommendation;
import technology.corvo.nfse.types.Webhook;

class CalculatePlansTest extends TestHarness {

	@Test
	void calculateReturnsTheEstimateAndOmitsAMissingCustomer() throws Exception {
		enqueueJson(
				200,
				"{\"data\":{\"preEmission\":true,\"status\":\"ready\","
						+ "\"taxes\":{\"iss\":{\"base\":\"15000.00\",\"rate\":\"3\",\"amount\":\"450.00\"},"
						+ "\"ibsTotal\":{\"base\":\"14550.00\",\"amount\":\"14.55\"}},"
						+ "\"netReceivableAmount\":\"15000.00\",\"blocking\":null}}");

		InvoiceCreateParams params =
				InvoiceCreateParams.of(
								null, InvoiceCreateParams.Service.ofDescription("Consultoria"), 15000.0, "2026-10-16")
						.withCompanyTaxId("11222333000181");
		Result<InvoiceCalculation> result = corvo().invoices().calculate(params).get(5, TimeUnit.SECONDS);

		InvoiceCalculation estimate = ((Result.Ok<InvoiceCalculation>) result).data();
		assertThat(estimate.status()).isEqualTo("ready");
		assertThat(estimate.taxes().iss().amount()).isEqualTo("450.00");
		assertThat(estimate.taxes().ibsTotal().rate()).isNull();
		RecordedRequest request = takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).isEqualTo("/v1/invoices/calculate");
		assertThat(request.getBody().readUtf8()).doesNotContain("customer");
	}

	@Test
	void calculateBlockedIsDataNotAnError() throws Exception {
		enqueueJson(
				200,
				"{\"data\":{\"status\":\"blocked\","
						+ "\"blocking\":{\"code\":\"TAX_RATE_NOT_FOUND\",\"message\":\"x\",\"step\":\"s\"},"
						+ "\"pendingFields\":[{\"field\":\"iss.rate\",\"reason\":\"missing\"}]}}");

		Result<InvoiceCalculation> result =
				corvo()
						.invoices()
						.calculate(InvoiceCreateParams.of(null, null, 10.0, "2026-10-16"))
						.get(5, TimeUnit.SECONDS);

		InvoiceCalculation estimate = ((Result.Ok<InvoiceCalculation>) result).data();
		assertThat(estimate.blocking().code()).isEqualTo("TAX_RATE_NOT_FOUND");
		assertThat(estimate.pendingFields()).hasSize(1);
	}

	@Test
	void plansCatalogListGetAndQuote() throws Exception {
		enqueueJson(
				200,
				"{\"data\":[{\"slug\":\"developer\",\"monthlyAllowance\":200,\"monthlyTotalCents\":2990}],"
						+ "\"pagination\":{\"limit\":5,\"hasMore\":false,\"nextCursor\":null}}");
		enqueueJson(200, "{\"data\":{\"slug\":\"growth\",\"monthlyTotalCents\":8990}}");
		enqueueJson(
				200, "{\"data\":{\"recommended_plan\":\"volume\",\"total_estimated_cents\":179990}}");
		enqueueJson(200, "{\"data\":null}");

		Result<Paginated<PlanQuote>> page = corvo().plans().list(5, null).get(5, TimeUnit.SECONDS);
		assertThat(((Result.Ok<Paginated<PlanQuote>>) page).data().data().get(0).slug())
				.isEqualTo("developer");
		assertThat(takeRequest().getPath()).isEqualTo("/v1/plans?limit=5");

		Result<PlanQuote> plan = corvo().plans().get("growth").get(5, TimeUnit.SECONDS);
		assertThat(((Result.Ok<PlanQuote>) plan).data().monthlyTotalCents()).isEqualTo(8990);

		Result<PlanRecommendation> quote = corvo().plans().quote(20000).get(5, TimeUnit.SECONDS);
		PlanRecommendation recommendation = ((Result.Ok<PlanRecommendation>) quote).data();
		assertThat(recommendation.recommendedPlan()).isEqualTo("volume");
		assertThat(recommendation.totalEstimatedCents()).isEqualTo(179990);
		takeRequest();
		assertThat(takeRequest().getPath()).isEqualTo("/v1/plans/quote?volume=20000");

		Result<PlanRecommendation> none = corvo().plans().quote(0).get(5, TimeUnit.SECONDS);
		assertThat(((Result.Ok<PlanRecommendation>) none).data()).isNull();
	}

	@Test
	void subscribableEventsExcludeWebhookTest() {
		assertThat(Webhook.WEBHOOK_EVENTS).hasSize(11);
		assertThat(Webhook.WEBHOOK_EVENTS)
				.contains("nfse.invoice.cancelled", "nfse.invoice.cancellation_rejected");
		assertThat(Webhook.WEBHOOK_EVENTS).contains("nfse.invoice.fiscal_discrepancy");
		assertThat(Webhook.WEBHOOK_EVENTS).doesNotContain("webhook.test");
	}

	private Corvo corvo() {
		return Corvo.builder().apiKey(API_KEY).baseUrl(server.url("/").toString()).build();
	}
}
