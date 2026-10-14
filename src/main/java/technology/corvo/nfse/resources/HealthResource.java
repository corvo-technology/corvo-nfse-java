package technology.corvo.nfse.resources;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.CompletableFuture;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;

/**
 * Public health checks. These endpoints return raw JSON without the {@code data} envelope, so
 * responses are unwrapped as {@link UnwrapMode#NONE}.
 */
public final class HealthResource {

	private final HttpTransport http;

	public HealthResource(HttpTransport http) {
		this.http = http;
	}

	/** Liveness probe: is the process up? */
	public CompletableFuture<Result<JsonNode>> live() {
		return http.request(HttpMethod.GET, "/health/live", null, null, null, UnwrapMode.NONE, JsonNode.class);
	}

	/** Readiness probe: can the API serve requests (DB, queue, etc.)? */
	public CompletableFuture<Result<JsonNode>> ready() {
		return http.request(
				HttpMethod.GET, "/health/ready", null, null, null, UnwrapMode.NONE, JsonNode.class);
	}
}
