package technology.corvo.nfse.types;

import java.util.List;

/** Input for {@code webhooks.create}. */
public record WebhookCreateParams(String url, List<String> events) {

	public static WebhookCreateParams of(String url, List<String> events) {
		return new WebhookCreateParams(url, events);
	}
}
