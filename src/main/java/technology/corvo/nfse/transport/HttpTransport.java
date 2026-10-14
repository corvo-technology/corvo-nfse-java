package technology.corvo.nfse.transport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import technology.corvo.nfse.error.CorvoApiError;
import technology.corvo.nfse.types.Paginated;
import technology.corvo.nfse.types.Pagination;

/**
 * HTTP layer for the Corvo NFS-e API, built on the JDK {@link HttpClient}.
 *
 * <p>JSON bodies are passed through unchanged — the SDK never re-cases server fields. All
 * failures (API, network, timeout) are mapped to {@link CorvoApiError} and returned inside
 * {@link Result.Err}; this class never throws for those cases.
 */
public final class HttpTransport {

	public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

	private static final Pattern FILENAME_PATTERN = Pattern.compile("filename=\"([^\"]+)\"");

	/** Raw bytes + explicit content type (used for multipart uploads). */
	public record RawBody(byte[] bytes, String contentType) {}

	private final HttpClient client;
	private final ObjectMapper mapper;
	private final String apiKey;
	private final String baseUrl;
	private final String prefix;
	private final Duration timeout;

	public HttpTransport(String apiKey, String baseUrl, String prefix, Duration timeout) {
		this.apiKey = apiKey;
		this.baseUrl = stripTrailingSlashes(baseUrl);
		this.prefix = stripSlashes(prefix == null || prefix.isEmpty() ? "v1" : prefix);
		this.timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
		this.client =
				HttpClient.newBuilder()
						.connectTimeout(this.timeout)
						.followRedirects(HttpClient.Redirect.NEVER)
						.build();
		// Mirror the Node SDK: nulls are omitted from request bodies (like JSON.stringify
		// dropping undefined) so partial PATCH bodies only carry the fields you set.
		this.mapper =
				new ObjectMapper()
						.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
						.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

	public ObjectMapper mapper() {
		return mapper;
	}

	/**
	 * Issues a request and unwraps the response envelope into {@code type}.
	 *
	 * @param body request body: {@code null} for none, {@link RawBody} for pre-encoded bodies
	 *     (multipart), any Jackson-serializable object for JSON
	 */
	public <T> CompletableFuture<Result<T>> request(
			HttpMethod method,
			String path,
			Object body,
			Map<String, Object> query,
			String idempotencyKey,
			UnwrapMode unwrap,
			Class<T> type) {
		HttpRequest httpRequest;
		try {
			httpRequest = buildRequest(method, path, body, query, idempotencyKey);
		} catch (JsonProcessingException e) {
			return CompletableFuture.completedFuture(
					Result.err(new CorvoApiError(0, "invalid_request_body", e.getMessage(), null, null, null)));
		}
		return send(httpRequest)
				.thenApply(
						raw -> {
							if (raw instanceof Result.Err<HttpResponse<byte[]>> err) {
								return Result.err(err.error());
							}
							HttpResponse<byte[]> response = ((Result.Ok<HttpResponse<byte[]>>) raw).data();
							if (!isSuccess(response.statusCode())) {
								return Result.err(toError(response));
							}
							if (response.statusCode() == 204 || response.body().length == 0) {
								return Result.ok(null);
							}
							Object parsed;
							try {
								parsed = mapper.readTree(response.body());
							} catch (IOException e) {
								return Result.err(invalidJson(response));
							}
							return Result.ok(convert(unwrap(parsed, unwrap), type));
						});
	}

	/** Issues a request and decodes a list page envelope into {@link Paginated}. */
	public <T> CompletableFuture<Result<Paginated<T>>> list(
			String path, Map<String, Object> query, UnwrapMode itemUnwrap, Class<T> itemType) {
		return request(HttpMethod.GET, path, null, query, null, UnwrapMode.NONE, JsonNode.class)
				.thenApply(
						page -> {
							if (page instanceof Result.Err<JsonNode> err) {
								return Result.err(err.error());
							}
							JsonNode body = ((Result.Ok<JsonNode>) page).data();
							Pagination pagination =
									convert(body == null ? null : body.get("pagination"), Pagination.class);
							List<T> items = new ArrayList<>();
							JsonNode data = body == null ? null : body.get("data");
							if (data != null && data.isArray()) {
								for (JsonNode item : data) {
									items.add(convert(unwrap(item, itemUnwrap), itemType));
								}
							}
							return Result.ok(
									new Paginated<>(
											items, pagination == null ? new Pagination(0, false, null) : pagination));
						});
	}

	/**
	 * Lazily walks every page of a cursor-paginated list, yielding items as a blocking
	 * {@link Stream}. Pages are fetched on demand as the stream is consumed.
	 *
	 * <p><b>Documented exception to the never-throw rule:</b> a page failure (API error, network
	 * error, or timeout) throws {@link CorvoApiError} from the stream — there is no yield channel
	 * to return a {@code Result} in.
	 */
	public <T> Stream<T> iterate(
			String path, Map<String, Object> query, UnwrapMode itemUnwrap, Class<T> itemType) {
		Iterator<T> iterator =
				new Iterator<>() {
					private final Map<String, Object> baseQuery =
							query == null ? new LinkedHashMap<>() : new LinkedHashMap<>(query);
					private Iterator<T> buffer = List.<T>of().iterator();
					private String cursor = initialCursor(baseQuery);
					private boolean done = false;

					@Override
					public boolean hasNext() {
						while (!buffer.hasNext() && !done) {
							fetchNextPage();
						}
						return buffer.hasNext();
					}

					@Override
					public T next() {
						if (!hasNext()) {
							throw new NoSuchElementException();
						}
						return buffer.next();
					}

					private void fetchNextPage() {
						Map<String, Object> pageQuery = new LinkedHashMap<>(baseQuery);
						if (cursor != null) {
							pageQuery.put("cursor", cursor);
						}
						Result<Paginated<T>> page = list(path, pageQuery, itemUnwrap, itemType).join();
						Paginated<T> data;
						if (page instanceof Result.Ok<Paginated<T>> ok) {
							data = ok.data();
						} else {
							throw ((Result.Err<Paginated<T>>) page).error();
						}
						buffer = data.data().iterator();
						if (!data.pagination().hasMore() || data.pagination().nextCursor() == null) {
							done = true;
						} else {
							cursor = data.pagination().nextCursor();
						}
					}
				};
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
	}

	/** Issues a request expecting a binary body (invoice PDF/XML). */
	public CompletableFuture<Result<DownloadedFile>> requestRaw(
			HttpMethod method, String path, Map<String, Object> query) {
		HttpRequest httpRequest;
		try {
			httpRequest = buildRequest(method, path, null, query, null);
		} catch (JsonProcessingException e) {
			return CompletableFuture.completedFuture(
					Result.err(new CorvoApiError(0, "invalid_request_body", e.getMessage(), null, null, null)));
		}
		return send(httpRequest)
				.thenApply(
						raw -> {
							if (raw instanceof Result.Err<HttpResponse<byte[]>> err) {
								return Result.err(err.error());
							}
							HttpResponse<byte[]> response = ((Result.Ok<HttpResponse<byte[]>>) raw).data();
							if (!isSuccess(response.statusCode())) {
								return Result.err(toError(response));
							}
							String disposition = header(response, "Content-Disposition");
							String fileName = null;
							if (disposition != null) {
								Matcher match = FILENAME_PATTERN.matcher(disposition);
								if (match.find()) {
									fileName = match.group(1);
								}
							}
							String mimeType = header(response, "Content-Type");
							return Result.ok(new DownloadedFile(response.body(), mimeType, fileName));
						});
	}

	// ------------------------------------------------------------------
	// Request construction and sending
	// ------------------------------------------------------------------

	private HttpRequest buildRequest(
			HttpMethod method, String path, Object body, Map<String, Object> query, String idempotencyKey)
			throws JsonProcessingException {
		HttpRequest.Builder builder =
				HttpRequest.newBuilder(buildUri(path, query))
						.timeout(timeout)
						.header("X-API-Key", apiKey)
						.header("Accept", "application/json");
		if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
			builder.header("Idempotency-Key", idempotencyKey);
		}
		HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.noBody();
		if (body instanceof RawBody raw) {
			publisher = HttpRequest.BodyPublishers.ofByteArray(raw.bytes());
			builder.header("Content-Type", raw.contentType());
		} else if (body != null) {
			publisher = HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(body));
			builder.header("Content-Type", "application/json");
		}
		builder.method(method.name(), publisher);
		return builder.build();
	}

	/** Sends the request, mapping transport failures into {@code Err} instead of throwing. */
	private CompletableFuture<Result<HttpResponse<byte[]>>> send(HttpRequest request) {
		return client
				.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
				.handle(
						(response, ex) -> {
							if (ex != null) {
								Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
								if (cause instanceof HttpTimeoutException) {
									return Result.<HttpResponse<byte[]>>err(CorvoApiError.timeout(timeout));
								}
								return Result.err(
										CorvoApiError.network("Network request failed: " + cause.getMessage()));
							}
							return Result.ok(response);
						});
	}

	private URI buildUri(String path, Map<String, Object> query) {
		String prefixPart = prefix.isEmpty() ? "" : "/" + prefix;
		StringBuilder url = new StringBuilder(baseUrl).append(prefixPart).append(path);
		if (query != null) {
			boolean first = true;
			for (Map.Entry<String, Object> entry : query.entrySet()) {
				if (entry.getValue() == null) {
					continue;
				}
				url.append(first ? '?' : '&');
				first = false;
				url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
				url.append('=');
				url.append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
			}
		}
		return URI.create(url.toString());
	}

	// ------------------------------------------------------------------
	// Response mapping
	// ------------------------------------------------------------------

	private static boolean isSuccess(int status) {
		return status >= 200 && status < 300;
	}

	private CorvoApiError toError(HttpResponse<byte[]> response) {
		int status = response.statusCode();
		String headerRequestId = header(response, "X-Request-Id");
		String retryAfterHeader = header(response, "Retry-After");
		JsonNode parsed = null;
		try {
			parsed = mapper.readTree(response.body());
		} catch (IOException ignored) {
			// non-JSON error body — fall back to header/status defaults
		}
		String code =
				parsed != null && parsed.path("code").isTextual() && !parsed.path("code").asText().isEmpty()
						? parsed.path("code").asText()
						: "http_" + status;
		String message;
		JsonNode messageNode = parsed == null ? null : parsed.get("message");
		if (messageNode != null && messageNode.isTextual()) {
			message = messageNode.asText();
		} else if (messageNode != null && messageNode.isArray()) {
			List<String> parts = new ArrayList<>();
			messageNode.forEach(part -> parts.add(part.asText()));
			message = String.join("; ", parts);
		} else if (parsed != null && parsed.path("error").isTextual()) {
			message = parsed.path("error").asText();
		} else {
			message = "Request failed with status " + status;
		}
		List<CorvoApiError.Detail> details = null;
		JsonNode detailsNode = parsed == null ? null : parsed.get("details");
		if (detailsNode != null && detailsNode.isArray()) {
			List<CorvoApiError.Detail> parsedDetails = new ArrayList<>();
			for (JsonNode detail : detailsNode) {
				parsedDetails.add(
						new CorvoApiError.Detail(
								detail.path("field").asText(null),
								detail.path("code").asText(null),
								detail.path("message").asText(null)));
			}
			details = parsedDetails;
		}
		String requestId =
				parsed != null && parsed.path("requestId").isTextual()
						? parsed.path("requestId").asText()
						: headerRequestId;
		Integer retryAfter = null;
		if (parsed != null && parsed.path("retryAfter").canConvertToInt()) {
			retryAfter = parsed.path("retryAfter").asInt();
		} else if (retryAfterHeader != null) {
			try {
				retryAfter = (int) Double.parseDouble(retryAfterHeader.trim());
			} catch (NumberFormatException ignored) {
				// non-numeric Retry-After — leave null
			}
		}
		return new CorvoApiError(status, code, message, details, requestId, retryAfter);
	}

	private CorvoApiError invalidJson(HttpResponse<byte[]> response) {
		return new CorvoApiError(
				response.statusCode(),
				CorvoApiError.CODE_INVALID_JSON,
				"Invalid JSON in response",
				null,
				header(response, "X-Request-Id"),
				null);
	}

	/** Implements the shared envelope unwrap: {@code data} / {@code data+props} / {@code none}. */
	static Object unwrap(Object value, UnwrapMode mode) {
		if (mode == UnwrapMode.NONE) {
			return value;
		}
		JsonNode node = value instanceof JsonNode json ? json : null;
		Object data = node != null && node.isObject() && node.has("data") ? node.get("data") : null;
		if (mode == UnwrapMode.DATA) {
			return data;
		}
		if (data instanceof JsonNode dataNode && dataNode.isObject() && dataNode.has("props")) {
			return dataNode.get("props");
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T> T convert(Object value, Class<T> type) {
		if (value == null || type == Void.class) {
			return null;
		}
		if (type.isInstance(value)) {
			return (T) value;
		}
		return mapper.convertValue(value, type);
	}

	private static String header(HttpResponse<byte[]> response, String name) {
		return response.headers().firstValue(name).orElse(null);
	}

	private static String stripTrailingSlashes(String value) {
		String out = value;
		while (out.endsWith("/")) {
			out = out.substring(0, out.length() - 1);
		}
		return out;
	}

	private static String stripSlashes(String value) {
		String out = stripTrailingSlashes(value);
		while (out.startsWith("/")) {
			out = out.substring(1);
		}
		return out;
	}

	private static String initialCursor(Map<String, Object> query) {
		Object cursor = query.get("cursor");
		return cursor == null ? null : String.valueOf(cursor);
	}
}
