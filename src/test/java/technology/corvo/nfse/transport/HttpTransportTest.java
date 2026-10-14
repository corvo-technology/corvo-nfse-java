package technology.corvo.nfse.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import technology.corvo.nfse.TestHarness;
import technology.corvo.nfse.error.CorvoApiError;
import technology.corvo.nfse.types.Paginated;

class HttpTransportTest extends TestHarness {

	record Sample(String id, String name) {}

	private static final Sample SAMPLE = new Sample("s_1", "hello");

	@Test
	void sendsApiKeyAndPrefixInPath() throws Exception {
		enqueueJson(200, "{\"data\":{\"id\":\"s_1\",\"name\":\"hello\"}}");

		Result<Sample> result =
				transport
						.request(
								HttpMethod.GET, "/samples", null, null, null, UnwrapMode.DATA, Sample.class)
						.get(5, TimeUnit.SECONDS);

		assertThat(result).isInstanceOf(Result.Ok.class);
		assertThat(((Result.Ok<Sample>) result).data()).isEqualTo(SAMPLE);

		RecordedRequest request = takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/v1/samples");
		assertThat(request.getHeader("X-API-Key")).isEqualTo(API_KEY);
		// Auth is X-API-Key — never Authorization: Bearer.
		assertThat(request.getHeader("Authorization")).isNull();
	}

	@Test
	void encodesQueryParametersAndSkipsNulls() throws Exception {
		enqueueJson(200, "{\"data\":null}");
		Map<String, Object> query = new LinkedHashMap<>();
		query.put("limit", 10);
		query.put("cursor", null);
		query.put("status", "authorized");

		transport.request(HttpMethod.GET, "/samples", null, query, null, UnwrapMode.DATA, Sample.class)
				.get(5, TimeUnit.SECONDS);

		RecordedRequest request = takeRequest();
		assertThat(request.getRequestUrl().queryParameter("limit")).isEqualTo("10");
		assertThat(request.getRequestUrl().queryParameter("status")).isEqualTo("authorized");
		assertThat(request.getRequestUrl().queryParameter("cursor")).isNull();
	}

	@Test
	void postsJsonBodyWithContentType() throws Exception {
		enqueueJson(200, "{\"data\":{\"id\":\"s_1\"}}");
		Sample body = new Sample("s_1", "posted");

		transport.request(HttpMethod.POST, "/samples", body, null, null, UnwrapMode.DATA, Sample.class)
				.get(5, TimeUnit.SECONDS);

		RecordedRequest request = takeRequest();
		assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
		assertThat(request.getBody().readUtf8()).isEqualTo("{\"id\":\"s_1\",\"name\":\"posted\"}");
	}

	@Test
	void sendsIdempotencyKeyHeader() throws Exception {
		enqueueJson(200, "{\"data\":null}");

		transport
				.request(
						HttpMethod.POST, "/samples", Map.of("a", 1), null, "key-42", UnwrapMode.DATA,
						Sample.class)
				.get(5, TimeUnit.SECONDS);

		assertThat(takeRequest().getHeader("Idempotency-Key")).isEqualTo("key-42");
	}

	@Test
	void unwrapsDataEnvelope() throws Exception {
		enqueueJson(200, "{\"data\":{\"id\":\"s_1\",\"name\":\"hello\"}}");

		Result<Sample> result =
				transport
						.request(HttpMethod.GET, "/samples/s_1", null, null, null, UnwrapMode.DATA, Sample.class)
						.get(5, TimeUnit.SECONDS);

		assertThat(((Result.Ok<Sample>) result).data()).isEqualTo(SAMPLE);
	}

	@Test
	void unwrapsDataPropsEnvelope() throws Exception {
		enqueueJson(200, "{\"data\":{\"props\":{\"id\":\"s_1\",\"name\":\"hello\"}}}");

		Result<Sample> result =
				transport
						.request(
								HttpMethod.GET, "/samples/s_1", null, null, null, UnwrapMode.DATA_PROPS,
								Sample.class)
						.get(5, TimeUnit.SECONDS);

		assertThat(((Result.Ok<Sample>) result).data()).isEqualTo(SAMPLE);
	}

	@Test
	void map204ToNull() throws Exception {
		server.enqueue(new MockResponse().setResponseCode(204));

		Result<Sample> result =
				transport
						.request(
								HttpMethod.DELETE, "/samples/s_1", null, null, null, UnwrapMode.DATA,
								Sample.class)
						.get(5, TimeUnit.SECONDS);

		assertThat(result).isInstanceOf(Result.Ok.class);
		assertThat(((Result.Ok<Sample>) result).data()).isNull();
	}

	@Test
	void mapsRestErrorWithCodeDetailsRequestId() throws Exception {
		enqueueJson(
				400,
				"{\"statusCode\":400,\"code\":\"ERROR_VALIDATION\",\"message\":\"Invalid body\","
						+ "\"details\":[{\"field\":\"taxId\",\"code\":\"invalid\",\"message\":\"bad taxId\"}],"
						+ "\"requestId\":\"req_123\"}");

		Result<Sample> result =
				transport
						.request(HttpMethod.POST, "/samples", Map.of(), null, null, UnwrapMode.DATA, Sample.class)
						.get(5, TimeUnit.SECONDS);

		assertThat(result).isInstanceOf(Result.Err.class);
		CorvoApiError error = ((Result.Err<Sample>) result).error();
		assertThat(error.status()).isEqualTo(400);
		assertThat(error.code()).isEqualTo("ERROR_VALIDATION");
		assertThat(error.getMessage()).isEqualTo("Invalid body");
		assertThat(error.requestId()).isEqualTo("req_123");
		assertThat(error.details())
				.containsExactly(new CorvoApiError.Detail("taxId", "invalid", "bad taxId"));
	}

	@Test
	void mapsRetryAfterFromHeaderOn429() throws Exception {
		server.enqueue(
				new MockResponse()
						.setResponseCode(429)
						.setHeader("Retry-After", "30")
						.setHeader("X-Request-Id", "req_9")
						.setBody("{\"code\":\"ERROR_RATE_LIMITED\",\"message\":\"Slow down\"}"));

		Result<Sample> result =
				transport
						.request(HttpMethod.GET, "/samples", null, null, null, UnwrapMode.DATA, Sample.class)
						.get(5, TimeUnit.SECONDS);

		CorvoApiError error = ((Result.Err<Sample>) result).error();
		assertThat(error.status()).isEqualTo(429);
		assertThat(error.code()).isEqualTo("ERROR_RATE_LIMITED");
		assertThat(error.retryAfter()).isEqualTo(30);
		assertThat(error.requestId()).isEqualTo("req_9");
	}

	@Test
	void mapsGoneWithoutCodeToHttpFallback() throws Exception {
		server.enqueue(new MockResponse().setResponseCode(410).setBody("{}"));

		Result<Sample> result =
				transport
						.request(HttpMethod.GET, "/samples", null, null, null, UnwrapMode.DATA, Sample.class)
						.get(5, TimeUnit.SECONDS);

		CorvoApiError error = ((Result.Err<Sample>) result).error();
		assertThat(error.status()).isEqualTo(410);
		assertThat(error.code()).isEqualTo("http_410");
	}

	@Test
	void mapsNetworkErrorToErrNotThrow() throws Exception {
		server.shutdown();

		Result<Sample> result =
				transport
						.request(HttpMethod.GET, "/samples", null, null, null, UnwrapMode.DATA, Sample.class)
						.get(5, TimeUnit.SECONDS);

		assertThat(result).isInstanceOf(Result.Err.class);
		CorvoApiError error = ((Result.Err<Sample>) result).error();
		assertThat(error.status()).isZero();
		assertThat(error.code()).isEqualTo("network_error");
	}

	@Test
	void mapsTimeoutToErrWithTimeoutCode() throws Exception {
		HttpTransport fastTimeout =
				new HttpTransport(API_KEY, server.url("/").toString(), "v1", Duration.ofMillis(100));
		server.enqueue(new MockResponse().setBodyDelay(2, TimeUnit.SECONDS).setBody("{}"));

		Result<Sample> result =
				fastTimeout
						.request(HttpMethod.GET, "/samples", null, null, null, UnwrapMode.DATA, Sample.class)
						.get(5, TimeUnit.SECONDS);

		assertThat(result).isInstanceOf(Result.Err.class);
		CorvoApiError error = ((Result.Err<Sample>) result).error();
		assertThat(error.status()).isZero();
		assertThat(error.code()).isEqualTo("timeout");
	}

	@Test
	void mapsInvalidJsonToErr() throws Exception {
		server.enqueue(
				new MockResponse()
						.setResponseCode(200)
						.setHeader("Content-Type", "application/json")
						.setBody("not json"));

		Result<Sample> result =
				transport
						.request(HttpMethod.GET, "/samples", null, null, null, UnwrapMode.DATA, Sample.class)
						.get(5, TimeUnit.SECONDS);

		assertThat(((Result.Err<Sample>) result).error().code()).isEqualTo("invalid_json");
	}

	@Test
	void listDecodesPaginationEnvelope() throws Exception {
		enqueueJson(
				200,
				"{\"data\":[{\"id\":\"s_1\",\"name\":\"a\"},{\"id\":\"s_2\",\"name\":\"b\"}],"
						+ "\"pagination\":{\"limit\":50,\"hasMore\":true,\"nextCursor\":\"cur_2\"}}");

		Result<Paginated<Sample>> result =
				transport.list("/samples", null, UnwrapMode.DATA, Sample.class).get(5, TimeUnit.SECONDS);

		Paginated<Sample> page = ((Result.Ok<Paginated<Sample>>) result).data();
		assertThat(page.data()).hasSize(2);
		assertThat(page.pagination().hasMore()).isTrue();
		assertThat(page.pagination().nextCursor()).isEqualTo("cur_2");
	}

	@Test
	void iterateWalksCursorsUntilExhausted() throws Exception {
		enqueueJson(
				200,
				"{\"data\":[{\"id\":\"s_1\",\"name\":\"a\"}],"
						+ "\"pagination\":{\"limit\":1,\"hasMore\":true,\"nextCursor\":\"cur_2\"}}");
		enqueueJson(
				200,
				"{\"data\":[{\"id\":\"s_2\",\"name\":\"b\"}],"
						+ "\"pagination\":{\"limit\":1,\"hasMore\":false,\"nextCursor\":null}}");

		List<Sample> all =
				transport.iterate("/samples", Map.of("limit", 1), UnwrapMode.DATA, Sample.class).toList();

		assertThat(all).extracting(Sample::id).containsExactly("s_1", "s_2");
		RecordedRequest first = takeRequest();
		assertThat(first.getRequestUrl().queryParameter("limit")).isEqualTo("1");
		RecordedRequest second = takeRequest();
		assertThat(second.getRequestUrl().queryParameter("cursor")).isEqualTo("cur_2");
	}

	@Test
	void iterateThrowsTypedErrorOnPageFailure() throws Exception {
		enqueueJson(
				200,
				"{\"data\":[{\"id\":\"s_1\",\"name\":\"a\"}],"
						+ "\"pagination\":{\"limit\":1,\"hasMore\":true,\"nextCursor\":\"cur_2\"}}");
		enqueueJson(500, "{\"code\":\"ERROR_INTERNAL\",\"message\":\"boom\"}");

		Stream<Sample> stream = transport.iterate("/samples", null, UnwrapMode.DATA, Sample.class);

		// Documented exception: iterate throws CorvoApiError on a page failure.
		assertThatThrownBy(() -> stream.forEach(item -> {}))
				.isInstanceOf(CorvoApiError.class)
				.extracting(t -> ((CorvoApiError) t).code())
				.isEqualTo("ERROR_INTERNAL");
	}

	@Test
	void requestRawCapturesBytesMimeTypeAndFileName() throws Exception {
		byte[] pdf = "%PDF-1.4 fake".getBytes(StandardCharsets.UTF_8);
		server.enqueue(
				new MockResponse()
						.setResponseCode(200)
						.setHeader("Content-Type", "application/pdf")
						.setHeader("Content-Disposition", "attachment; filename=\"nfse_123.pdf\"")
						.setBody(new okio.Buffer().write(pdf)));

		Result<DownloadedFile> result =
				transport.requestRaw(HttpMethod.GET, "/invoices/inv_1/pdf", null).get(5, TimeUnit.SECONDS);

		DownloadedFile file = ((Result.Ok<DownloadedFile>) result).data();
		assertThat(file.content()).isEqualTo(pdf);
		assertThat(file.mimeType()).isEqualTo("application/pdf");
		assertThat(file.fileName()).isEqualTo("nfse_123.pdf");
	}
}
