package technology.corvo.nfse.resources;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import technology.corvo.nfse.Corvo;
import technology.corvo.nfse.TestHarness;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.types.WebhookCreateParams;
import technology.corvo.nfse.types.WebhookDelivery;
import technology.corvo.nfse.types.WebhookDeliveryStats;
import technology.corvo.nfse.types.WebhookWithSecret;

class WebhooksResourceTest extends TestHarness {

	private static final String WEBHOOK_JSON =
			"{\"id\":\"wh_1\",\"url\":\"https://example.com/hook\","
					+ "\"events\":[\"nfse.invoice.authorized\"],\"active\":true,"
					+ "\"createdAt\":\"2026-10-01T00:00:00Z\",\"updatedAt\":\"2026-10-01T00:00:00Z\"}";

	@Test
	void createReturnsSecretExactlyOnce() throws Exception {
		enqueueJson(200,
				"{\"data\":" + WEBHOOK_JSON.replace("}", ",\"secret\":\"whsec_abc\"}") + "}");

		Result<WebhookWithSecret> result =
				corvo().webhooks()
						.create(WebhookCreateParams.of("https://example.com/hook",
								List.of("nfse.invoice.authorized")))
						.get(5, TimeUnit.SECONDS);

		WebhookWithSecret webhook = ((Result.Ok<WebhookWithSecret>) result).data();
		assertThat(webhook.secret()).isEqualTo("whsec_abc");
		assertThat(webhook.events()).containsExactly("nfse.invoice.authorized");
		assertThat(takeRequest().getPath()).isEqualTo("/v1/webhooks");
	}

	@Test
	void deliveriesStatsAndRetry() throws Exception {
		enqueueJson(200, "{\"data\":{\"total\":5,\"sent\":3,\"failed\":1,\"pending\":1,\"delayed\":0}}");
		enqueueJson(200, "{\"data\":{\"retried\":true}}");

		Result<WebhookDeliveryStats> stats =
				corvo().webhooks().deliveries().stats(24, "test").get(5, TimeUnit.SECONDS);
		assertThat(((Result.Ok<WebhookDeliveryStats>) stats).data().failed()).isEqualTo(1);

		RecordedRequest statsRequest = takeRequest();
		assertThat(statsRequest.getPath()).startsWith("/v1/webhooks/deliveries/stats");
		assertThat(statsRequest.getRequestUrl().queryParameter("hours")).isEqualTo("24");
		assertThat(statsRequest.getRequestUrl().queryParameter("environment")).isEqualTo("test");

		Result<WebhooksResource.Deliveries.RetryResult> retry =
				corvo().webhooks().deliveries().retry("del_1").get(5, TimeUnit.SECONDS);
		assertThat(((Result.Ok<WebhooksResource.Deliveries.RetryResult>) retry).data().retried()).isTrue();
		assertThat(takeRequest().getPath()).isEqualTo("/v1/webhooks/deliveries/del_1/retry");
	}

	@Test
	void deliveriesListUnwrapsData() throws Exception {
		enqueueJson(
				200,
				"{\"data\":[{\"id\":\"del_1\",\"tenantId\":\"t_1\",\"webhookConfigId\":\"wh_1\","
						+ "\"eventType\":\"nfse.invoice.authorized\",\"environment\":\"test\","
						+ "\"url\":\"https://example.com/hook\",\"payload\":{},\"statusCode\":200,"
						+ "\"responseBody\":null,\"errorMessage\":null,\"status\":\"delivered\","
						+ "\"durationMs\":12,\"retryCount\":0,\"nextRetryAt\":null,\"lastError\":null,"
						+ "\"createdAt\":\"2026-10-01T00:00:00Z\"}],"
						+ "\"pagination\":{\"limit\":50,\"hasMore\":false,\"nextCursor\":null}}");

		Result<technology.corvo.nfse.types.Paginated<WebhookDelivery>> result =
				corvo().webhooks().deliveries().list(24, "test", null, null).get(5, TimeUnit.SECONDS);

		assertThat(((Result.Ok<technology.corvo.nfse.types.Paginated<WebhookDelivery>>) result).data().data())
				.hasSize(1);
	}

	@Test
	void testAndRemove() throws Exception {
		enqueueJson(200, "{\"data\":{\"sent\":true}}");
		enqueueJson(200, "{\"data\":{\"deleted\":true}}");

		corvo().webhooks().test("wh_1").get(5, TimeUnit.SECONDS);
		corvo().webhooks().remove("wh_1").get(5, TimeUnit.SECONDS);

		assertThat(takeRequest().getPath()).isEqualTo("/v1/webhooks/wh_1/test");
		assertThat(takeRequest().getPath()).isEqualTo("/v1/webhooks/wh_1");
	}

	private Corvo corvo() {
		return Corvo.builder().apiKey(API_KEY).baseUrl(server.url("/").toString()).build();
	}
}
