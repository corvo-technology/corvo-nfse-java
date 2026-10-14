package technology.corvo.nfse.resources;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.Paginated;
import technology.corvo.nfse.types.Webhook;
import technology.corvo.nfse.types.WebhookCreateParams;
import technology.corvo.nfse.types.WebhookDelivery;
import technology.corvo.nfse.types.WebhookDeliveryStats;
import technology.corvo.nfse.types.WebhookWithSecret;

/** Webhook configuration and delivery operations. */
public final class WebhooksResource {

	private static final String BASE = "/webhooks";

	/** Delivery sub-resource: {@code corvo.webhooks().deliveries()}. */
	public static final class Deliveries {

		private static final String DELIVERIES_BASE = BASE + "/deliveries";

		private final HttpTransport http;

		Deliveries(HttpTransport http) {
			this.http = http;
		}

		/** Lists one page of delivery attempts. */
		public CompletableFuture<Result<Paginated<WebhookDelivery>>> list(
				Integer hours, String environment, Integer limit, String cursor) {
			return http.list(DELIVERIES_BASE, query(hours, environment, limit, cursor), UnwrapMode.DATA,
					WebhookDelivery.class);
		}

		/**
		 * Walks every page of deliveries lazily.
		 *
		 * <p><b>Documented exception to the never-throw rule:</b> if fetching a page fails, a
		 * {@code CorvoApiError} is thrown from the stream.
		 */
		public Stream<WebhookDelivery> iterate(
				Integer hours, String environment, Integer limit, String cursor) {
			return http.iterate(DELIVERIES_BASE, query(hours, environment, limit, cursor), UnwrapMode.DATA,
					WebhookDelivery.class);
		}

		/** Aggregated delivery counters for a time window. */
		public CompletableFuture<Result<WebhookDeliveryStats>> stats(Integer hours, String environment) {
			Map<String, Object> query = new LinkedHashMap<>();
			query.put("hours", hours);
			query.put("environment", environment);
			return http.request(HttpMethod.GET, DELIVERIES_BASE + "/stats", null, query, null,
					UnwrapMode.DATA, WebhookDeliveryStats.class);
		}

		/** Requeues a failed delivery for another attempt. */
		public CompletableFuture<Result<RetryResult>> retry(String deliveryId) {
			return http.request(
					HttpMethod.POST, DELIVERIES_BASE + "/" + CompaniesResource.encode(deliveryId) + "/retry",
					null, null, null, UnwrapMode.DATA, RetryResult.class);
		}

		/** Payload of {@code deliveries.retry}. */
		public record RetryResult(boolean retried) {}

		private static Map<String, Object> query(Integer hours, String environment, Integer limit, String cursor) {
			Map<String, Object> query = new LinkedHashMap<>();
			query.put("hours", hours);
			query.put("environment", environment);
			query.put("limit", limit);
			query.put("cursor", cursor);
			return query;
		}
	}

	private final HttpTransport http;
	private final Deliveries deliveries;

	public WebhooksResource(HttpTransport http) {
		this.http = http;
		this.deliveries = new Deliveries(http);
	}

	/** The deliveries sub-resource. */
	public Deliveries deliveries() {
		return deliveries;
	}

	/** Lists one page of webhook configurations. */
	public CompletableFuture<Result<Paginated<Webhook>>> list(Integer limit, String cursor) {
		return http.list(BASE, CompaniesResource.listQuery(limit, cursor), UnwrapMode.DATA, Webhook.class);
	}

	/**
	 * Walks every page of webhook configurations lazily.
	 *
	 * <p><b>Documented exception to the never-throw rule:</b> if fetching a page fails, a
	 * {@code CorvoApiError} is thrown from the stream.
	 */
	public Stream<Webhook> iterate(Integer limit, String cursor) {
		return http.iterate(BASE, CompaniesResource.listQuery(limit, cursor), UnwrapMode.DATA, Webhook.class);
	}

	/** Creates a webhook. The signing {@code secret} is returned exactly once, here. */
	public CompletableFuture<Result<WebhookWithSecret>> create(WebhookCreateParams params) {
		return http.request(HttpMethod.POST, BASE, params, null, null, UnwrapMode.DATA,
				WebhookWithSecret.class);
	}

	/** Deletes a webhook. */
	public CompletableFuture<Result<DeletedResult>> remove(String id) {
		return http.request(
				HttpMethod.DELETE, BASE + "/" + CompaniesResource.encode(id), null, null, null,
				UnwrapMode.DATA, DeletedResult.class);
	}

	/** Sends a test event to the webhook. */
	public CompletableFuture<Result<SentResult>> test(String id) {
		return http.request(
				HttpMethod.POST, BASE + "/" + CompaniesResource.encode(id) + "/test", null, null, null,
				UnwrapMode.DATA, SentResult.class);
	}

	/** Payload of {@code webhooks.remove}. */
	public record DeletedResult(boolean deleted) {}

	/** Payload of {@code webhooks.test}. */
	public record SentResult(boolean sent) {}
}
