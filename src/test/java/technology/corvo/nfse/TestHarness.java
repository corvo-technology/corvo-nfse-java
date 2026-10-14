package technology.corvo.nfse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import technology.corvo.nfse.transport.HttpTransport;

/** Shared MockWebServer harness for unit tests. */
public abstract class TestHarness {

	public static final String API_KEY = "corvo_test_" + "a".repeat(40);

	protected MockWebServer server;
	protected HttpTransport transport;

	@BeforeEach
	public void startServer() throws IOException {
		server = new MockWebServer();
		server.start();
		transport = new HttpTransport(API_KEY, server.url("/").toString(), "v1", HttpTransport.DEFAULT_TIMEOUT);
	}

	@AfterEach
	public void stopServer() throws IOException {
		server.shutdown();
	}

	protected void enqueueJson(int status, String body) {
		server.enqueue(
				new MockResponse()
						.setResponseCode(status)
						.setHeader("Content-Type", "application/json")
						.setBody(body));
	}

	protected RecordedRequest takeRequest() throws InterruptedException {
		RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
		if (request == null) {
			throw new AssertionError("No request recorded");
		}
		return request;
	}
}
