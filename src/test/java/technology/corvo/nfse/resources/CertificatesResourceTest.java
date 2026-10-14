package technology.corvo.nfse.resources;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import technology.corvo.nfse.Corvo;
import technology.corvo.nfse.TestHarness;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.types.Certificate;

class CertificatesResourceTest extends TestHarness {

	private static final String CERT_JSON =
			"{\"id\":\"cert_1\",\"companyId\":\"comp_1\",\"serialNumber\":\"01:02\",\"issuer\":\"ICP-Brasil\","
					+ "\"subject\":\"CN=Acme\",\"fingerprint\":\"ab:cd\",\"validFrom\":\"2026-01-01\","
					+ "\"validUntil\":\"2027-01-01\",\"status\":\"active\","
					+ "\"createdAt\":\"2026-10-01T00:00:00Z\"}";

	@Test
	void uploadSendsMultipartWithFileAndPassword() throws Exception {
		enqueueJson(200, "{\"data\":" + CERT_JSON + "}");
		byte[] p12 = new byte[] {0x30, (byte) 0x82, 0x01, 0x02};

		Result<Certificate> result =
				corvo().certificates()
						.upload("comp_1", p12, "hunter2", "acme.p12")
						.get(5, TimeUnit.SECONDS);

		assertThat(((Result.Ok<Certificate>) result).data().id()).isEqualTo("cert_1");
		RecordedRequest request = takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).isEqualTo("/v1/companies/comp_1/certificates");
		String contentType = request.getHeader("Content-Type");
		assertThat(contentType).startsWith("multipart/form-data; boundary=");
		String boundary = contentType.substring(contentType.indexOf('=') + 1);
		String body = request.getBody().readUtf8();
		assertThat(body).contains("--" + boundary);
		assertThat(body).contains("name=\"file\"; filename=\"acme.p12\"");
		assertThat(body).contains("Content-Type: application/x-pkcs12");
		assertThat(body).contains("name=\"password\"");
		assertThat(body).contains("hunter2");
		// Body ends with the closing boundary
		assertThat(body.trim()).endsWith("--" + boundary + "--");
	}

	@Test
	void uploadDefaultsFileName() throws Exception {
		enqueueJson(200, "{\"data\":" + CERT_JSON + "}");

		corvo().certificates().upload("comp_1", new byte[] {1}, "pw", null).get(5, TimeUnit.SECONDS);

		assertThat(takeRequest().getBody().readUtf8()).contains("filename=\"certificate.p12\"");
	}

	@Test
	void listAndRemoveUseNestedPaths() throws Exception {
		enqueueJson(200, "{\"data\":[" + CERT_JSON + "],\"pagination\":{\"limit\":50,\"hasMore\":false,"
				+ "\"nextCursor\":null}}");
		enqueueJson(204, "");

		corvo().certificates().list("comp_1", null, null).get(5, TimeUnit.SECONDS);
		corvo().certificates().remove("comp_1", "cert_1").get(5, TimeUnit.SECONDS);

		assertThat(takeRequest().getPath()).isEqualTo("/v1/companies/comp_1/certificates");
		assertThat(takeRequest().getPath()).isEqualTo("/v1/companies/comp_1/certificates/cert_1");
	}

	private Corvo corvo() {
		return Corvo.builder().apiKey(API_KEY).baseUrl(server.url("/").toString()).build();
	}
}
