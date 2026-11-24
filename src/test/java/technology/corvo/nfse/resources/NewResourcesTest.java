package technology.corvo.nfse.resources;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import technology.corvo.nfse.Corvo;
import technology.corvo.nfse.TestHarness;
import technology.corvo.nfse.Version;
import technology.corvo.nfse.error.CorvoApiError;
import technology.corvo.nfse.transport.DownloadedFile;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.types.*;

/** Request and response details of the operations added in contract 0.3.0. */
class NewResourcesTest extends TestHarness {

	private Corvo corvo() {
		return Corvo.builder().apiKey(API_KEY).baseUrl(server.url("/").toString()).build();
	}

	private static <T> T ok(Result<T> result) {
		assertThat(result).isInstanceOf(Result.Ok.class);
		return ((Result.Ok<T>) result).data();
	}

	private <T> T await(java.util.concurrent.CompletableFuture<Result<T>> f) throws Exception {
		return ok(f.get(5, TimeUnit.SECONDS));
	}

	@Test
	void versionMatchesThePomAndGoesInTheUserAgent() throws Exception {
		String pom = java.nio.file.Files.readString(java.nio.file.Path.of("pom.xml"));
		assertThat(pom).contains("<version>" + Version.VERSION + "</version>");
		enqueueJson(200, "{\"data\":{\"available\":true}}");
		await(corvo().fiscalPortal().status());
		assertThat(takeRequest().getHeader("User-Agent")).isEqualTo("corvo-nfse-java/0.3.0");
	}

	@Test
	void cnpjLookupParsesTheProfileAndErrorsComeBackAsErr() throws Exception {
		enqueueJson(200, "{\"data\":{\"legalName\":\"ACME LTDA\",\"mei\":false,\"simples\":true,"
				+ "\"address\":{\"city\":\"Sao Paulo\"}}}");
		enqueueJson(422, "{\"statusCode\":422,\"code\":\"CNPJ_INVALID\",\"message\":\"bad\",\"requestId\":\"r1\"}");
		CnpjProfile profile = await(corvo().cnpj().lookup("12345678000195"));
		assertThat(profile.legalName()).isEqualTo("ACME LTDA");
		assertThat(profile.simples()).isTrue();
		assertThat(takeRequest().getPath()).isEqualTo("/v1/cnpj/12345678000195");
		Result<CnpjProfile> err = corvo().cnpj().lookup("1").get(5, TimeUnit.SECONDS);
		CorvoApiError error = ((Result.Err<CnpjProfile>) err).error();
		assertThat(error.code()).isEqualTo("CNPJ_INVALID");
		assertThat(error.requestId()).isEqualTo("r1");
	}

	@Test
	void rateLimitErrorKeepsRetryAfter() throws Exception {
		server.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", "7")
				.setHeader("Content-Type", "application/json")
				.setBody("{\"statusCode\":429,\"code\":\"ERROR_RATE_LIMITED\",\"message\":\"slow\"}"));
		Result<SavedCustomer> r = corvo().customers().get("12345678909").get(5, TimeUnit.SECONDS);
		assertThat(((Result.Err<SavedCustomer>) r).error().retryAfter()).isEqualTo(7);
	}

	@Test
	void customersSearchPaginatesAndIterates() throws Exception {
		enqueueJson(200, "{\"data\":[{\"taxId\":\"1\",\"name\":\"A\"}],"
				+ "\"pagination\":{\"limit\":1,\"hasMore\":true,\"nextCursor\":\"1\"}}");
		enqueueJson(200, "{\"data\":[{\"taxId\":\"2\",\"name\":\"B\"}],"
				+ "\"pagination\":{\"limit\":1,\"hasMore\":false,\"nextCursor\":null}}");
		List<SavedCustomer> all = corvo().customers().iterate("a", 1, null).toList();
		assertThat(all).extracting(SavedCustomer::taxId).containsExactly("1", "2");
		RecordedRequest first = takeRequest();
		assertThat(first.getRequestUrl().queryParameter("search")).isEqualTo("a");
		assertThat(takeRequest().getRequestUrl().queryParameter("cursor")).isEqualTo("1");
	}

	@Test
	void customersUpdateCanClearTheEmailWithAnExplicitNull() throws Exception {
		enqueueJson(200, "{\"data\":{\"taxId\":\"1\"}}");
		enqueueJson(200, "{\"data\":{\"taxId\":\"1\"}}");
		enqueueJson(200, "{\"data\":null}");
		Map<String, Object> patch = new HashMap<>();
		patch.put("email", null);
		await(corvo().customers().update("1", patch));
		assertThat(takeRequest().getBody().readUtf8()).isEqualTo("{\"email\":null}");
		await(corvo().customers().update("1", new UpdateCustomer(null, "Novo")));
		assertThat(takeRequest().getBody().readUtf8()).isEqualTo("{\"name\":\"Novo\"}");
		await(corvo().customers().remove("1"));
		assertThat(takeRequest().getMethod()).isEqualTo("DELETE");
	}

	@Test
	void serviceFavoritesListCreateRemove() throws Exception {
		enqueueJson(200, "{\"data\":[{\"id\":\"f1\",\"label\":\"Mensalidade\",\"amountCents\":9900}]}");
		enqueueJson(201, "{\"data\":{\"id\":\"f2\"}}");
		enqueueJson(200, "{\"data\":{\"deleted\":true}}");
		assertThat(await(corvo().serviceFavorites().list("c1")).get(0).amountCents()).isEqualTo(9900);
		await(corvo().serviceFavorites().create("c1",
				new ObjectMapper().readValue("{\"label\":\"L\",\"description\":\"D\",\"serviceCode\":\"010101\"}",
						CreateServiceFavorite.class)));
		RecordedRequest list = takeRequest();
		assertThat(list.getPath()).isEqualTo("/v1/companies/c1/service-favorites");
		assertThat(takeRequest().getBody().readUtf8()).contains("\"serviceCode\":\"010101\"");
		assertThat(await(corvo().serviceFavorites().remove("c1", "f2")).deleted()).isTrue();
	}

	@Test
	void accountantPackageUpdateSendsNullToTurnItOff() throws Exception {
		enqueueJson(200, "{\"data\":{\"email\":null,\"testEmailSent\":false}}");
		enqueueJson(200, "{\"data\":{\"status\":\"sent\"}}");
		enqueueJson(200, "{\"data\":[{\"id\":\"s1\",\"month\":\"2026-10\",\"status\":\"sent\"}]}");
		await(corvo().accountantPackage().update("c1", null));
		RecordedRequest put = takeRequest();
		assertThat(put.getMethod()).isEqualTo("PUT");
		assertThat(put.getBody().readUtf8()).isEqualTo("{\"accountantEmail\":null}");
		assertThat(await(corvo().accountantPackage().send("c1", "2026-10")).status()).isEqualTo("sent");
		assertThat(takeRequest().getBody().readUtf8()).isEqualTo("{\"month\":\"2026-10\"}");
		assertThat(await(corvo().accountantPackage().listSends("c1"))).hasSize(1);
	}

	@Test
	void invoicePackageSummaryAndZipDownload() throws Exception {
		enqueueJson(200, "{\"data\":{\"count\":3,\"limit\":500,\"month\":\"2026-10\",\"total\":450.5}}");
		server.enqueue(new MockResponse().setHeader("Content-Type", "application/zip")
				.setHeader("Content-Disposition", "attachment; filename=\"notas-2026-10.zip\"").setBody("PK"));
		assertThat(await(corvo().invoicePackage().summary("c1", "2026-10")).count()).isEqualTo(3);
		assertThat(takeRequest().getRequestUrl().queryParameter("month")).isEqualTo("2026-10");
		DownloadedFile zip = await(corvo().invoicePackage().download("c1", "2026-10", "xml"));
		assertThat(zip.fileName()).isEqualTo("notas-2026-10.zip");
		assertThat(zip.content()).isEqualTo("PK".getBytes());
		assertThat(takeRequest().getRequestUrl().queryParameter("include")).isEqualTo("xml");
	}

	@Test
	void dpsSequenceSet() throws Exception {
		enqueueJson(200, "{\"data\":{\"lastNumber\":120,\"series\":\"1\"}}");
		assertThat(await(corvo().dpsSequence().set("c1", 120)).lastNumber()).isEqualTo(120);
		RecordedRequest r = takeRequest();
		assertThat(r.getMethod()).isEqualTo("PUT");
		assertThat(r.getBody().readUtf8()).isEqualTo("{\"lastNumber\":120.0}");
	}

	@Test
	void serviceTaxProfileUpdateWithRawPatchClearsAField() throws Exception {
		enqueueJson(200, "{\"data\":{\"id\":\"s1\"}}");
		Map<String, Object> patch = new HashMap<>();
		patch.put("effectiveUntil", null);
		await(corvo().serviceTaxProfiles().update("c1", "s1", patch));
		RecordedRequest r = takeRequest();
		assertThat(r.getMethod()).isEqualTo("PATCH");
		assertThat(r.getBody().readUtf8()).isEqualTo("{\"effectiveUntil\":null}");
	}

	@Test
	void creatingAScheduledOrRecurringInvoiceReturnsTheSchedule() throws Exception {
		enqueueJson(202, "{\"data\":{\"id\":\"sch1\",\"kind\":\"recurring\",\"next_run_on\":\"2026-12-01\","
				+ "\"company_id\":\"c1\",\"status\":\"active\"}}");
		InvoiceCreateParams params =
				InvoiceCreateParams.of(
								InvoiceCreateParams.Customer.ofForeign("Acme Inc", new ForeignCustomer(
										"NYC", null, "US", null, null, "10", "10001", "NY", "5th", "123")),
								InvoiceCreateParams.Service.ofDescription("Consulting"), 100.0, "2026-12-01")
						.withCompanyId("c1")
						.withPurchaseOrder("PO-1")
						.withNotes("parcela 1")
						.withOptions(InvoiceCreateParams.Options.recurring("MONTHLY", "2027-12-01"));
		InvoiceCreateResult result = await(corvo().invoices().create(params, "k1"));
		assertThat(result.isSchedule()).isTrue();
		assertThat(result.invoice()).isNull();
		assertThat(result.schedule().nextRunOn()).isEqualTo("2026-12-01");
		assertThat(result.schedule().companyId()).isEqualTo("c1");
		RecordedRequest r = takeRequest();
		assertThat(r.getHeader("Idempotency-Key")).isEqualTo("k1");
		assertThat(r.getBody().readUtf8())
				.contains("\"purchaseOrder\":\"PO-1\"")
				.contains("\"notes\":\"parcela 1\"")
				.contains("\"type\":\"RECURRING\"")
				.contains("\"recurrence\":{\"endsOn\":\"2027-12-01\",\"frequency\":\"MONTHLY\"}")
				.contains("\"foreign\":{")
				.doesNotContain("\"taxId\":null");
	}

	@Test
	void schedulesListRunsAndActions() throws Exception {
		enqueueJson(200, "{\"data\":[{\"id\":\"s1\",\"status\":\"active\"}],"
				+ "\"pagination\":{\"limit\":50,\"hasMore\":false,\"nextCursor\":null}}");
		enqueueJson(200, "{\"data\":{\"id\":\"s1\",\"status\":\"paused\"}}");
		enqueueJson(200, "{\"data\":[{\"id\":\"r1\",\"outcome\":\"issued\",\"invoice_id\":\"i1\"}],"
				+ "\"pagination\":{\"limit\":50,\"hasMore\":false,\"nextCursor\":null}}");
		assertThat(await(corvo().schedules().list("active", null, null, null)).data()).hasSize(1);
		assertThat(takeRequest().getRequestUrl().queryParameter("status")).isEqualTo("active");
		assertThat(await(corvo().schedules().pause("s1")).status()).isEqualTo("paused");
		assertThat(takeRequest().getPath()).isEqualTo("/v1/schedules/s1/pause");
		assertThat(await(corvo().schedules().listRuns("s1", null, null)).data().get(0).invoiceId()).isEqualTo("i1");
	}

	@Test
	void importsStartSendsMultipartWithFileAndCompany() throws Exception {
		enqueueJson(202, "{\"data\":{\"id\":\"imp1\",\"status\":\"validating\"}}");
		ImportStart started = await(corvo().imports().start("a;b\n1;2\n".getBytes(), "notas.csv", "c1"));
		assertThat(started.id()).isEqualTo("imp1");
		RecordedRequest r = takeRequest();
		assertThat(r.getHeader("Content-Type")).startsWith("multipart/form-data; boundary=");
		String body = r.getBody().readUtf8();
		assertThat(body).contains("name=\"companyId\"").contains("c1")
				.contains("name=\"file\"; filename=\"notas.csv\"").contains("a;b");
	}

	@Test
	void importsRowsEmitAndCsvDownloads() throws Exception {
		enqueueJson(200, "{\"data\":[{\"line\":2,\"status\":\"invalid\",\"error_code\":\"X\"}],"
				+ "\"pagination\":{\"limit\":50,\"hasMore\":false,\"nextCursor\":null}}");
		enqueueJson(202, "{\"data\":{\"id\":\"imp1\",\"status\":\"emitting\",\"rows_expected\":4,"
				+ "\"counters\":{}}}");
		server.enqueue(new MockResponse().setHeader("Content-Type", "text/csv").setBody("a,b"));
		Paginated<ImportRow> rows = await(corvo().imports().listRows("imp1", "invalid", 50, null));
		assertThat(rows.data().get(0).errorCode()).isEqualTo("X");
		assertThat(takeRequest().getRequestUrl().queryParameter("status")).isEqualTo("invalid");
		assertThat(await(corvo().imports().emit("imp1", true)).rowsExpected()).isEqualTo(4);
		assertThat(takeRequest().getBody().readUtf8()).isEqualTo("{\"sendEmail\":true}");
		assertThat(new String(await(corvo().imports().downloadTemplate()).content())).isEqualTo("a,b");
	}

	@Test
	void integrationsConnectionsRulesAndOperations() throws Exception {
		enqueueJson(201, "{\"data\":{\"id\":\"cn1\",\"state\":\"draft\",\"secrets\":{\"webhookSecret\":\"s\"}}}");
		enqueueJson(200, "{\"data\":[{\"id\":\"r1\"}]}");
		enqueueJson(200, "{\"data\":{\"state\":\"processed\"}}");
		IntegrationConnectionCreated created = await(corvo().integrations().connections().create(
				new ObjectMapper().readValue("{\"provider\":\"stripe\",\"companyId\":\"c1\"}", CreateConnection.class)));
		assertThat(created.secrets()).containsEntry("webhookSecret", "s");
		assertThat(takeRequest().getPath()).isEqualTo("/v1/integrations/connections");
		assertThat(await(corvo().integrations().connections().replaceRules("cn1", List.of()))).hasSize(1);
		RecordedRequest put = takeRequest();
		assertThat(put.getMethod()).isEqualTo("PUT");
		assertThat(put.getBody().readUtf8()).isEqualTo("{\"rules\":[]}");
		assertThat(await(corvo().integrations().operations().retry("op1")).state()).isEqualTo("processed");
	}

	@Test
	void invoicesSubstituteOverviewReviewEmailAndCustomerFilter() throws Exception {
		String inv = "{\"id\":\"i2\",\"status\":\"queued\",\"substitutesInvoiceId\":\"i1\"}";
		enqueueJson(202, "{\"data\":" + inv + "}");
		enqueueJson(200, "{\"data\":{\"period\":\"30d\",\"counts\":{},\"dailyActivity\":[],\"recentInvoices\":[]}}");
		enqueueJson(202, "{\"data\":{\"results\":[{\"id\":\"i1\",\"outcome\":\"accepted\"}]}}");
		enqueueJson(202, "{\"data\":{\"emailId\":\"e1\",\"status\":\"queued\"}}");
		enqueueJson(200, "{\"data\":[],\"pagination\":{\"limit\":50,\"hasMore\":false,\"nextCursor\":null}}");

		Invoice sub = await(corvo().invoices().substitute("i1",
				new ObjectMapper().readValue("{\"description\":\"corrigir valor\"}", SubstituteInvoice.class), "k9"));
		assertThat(sub.substitutesInvoiceId()).isEqualTo("i1");
		RecordedRequest r = takeRequest();
		assertThat(r.getPath()).isEqualTo("/v1/invoices/i1/substitute");
		assertThat(r.getHeader("Idempotency-Key")).isEqualTo("k9");

		assertThat(await(corvo().invoices().overview("30d")).period()).isEqualTo("30d");
		assertThat(takeRequest().getRequestUrl().queryParameter("period")).isEqualTo("30d");

		assertThat(await(corvo().invoices().review("retry", List.of("i1"))).results().get(0).outcome())
				.isEqualTo("accepted");
		assertThat(takeRequest().getBody().readUtf8()).isEqualTo("{\"action\":\"retry\",\"invoiceIds\":[\"i1\"]}");

		await(corvo().invoices().sendEmail("i1", "outro@example.com", "k8"));
		RecordedRequest email = takeRequest();
		assertThat(email.getBody().readUtf8()).isEqualTo("{\"email\":\"outro@example.com\"}");
		assertThat(email.getHeader("Idempotency-Key")).isEqualTo("k8");

		await(corvo().invoices().list(null, null, null, null, null, "12345678909", null, null));
		assertThat(takeRequest().getRequestUrl().queryParameter("customerTaxId")).isEqualTo("12345678909");
	}

	@Test
	void creditsPreviewPurchaseIsAGetWithUnits() throws Exception {
		enqueueJson(200, "{\"data\":{\"units\":10,\"unitPriceCents\":99,\"totalCents\":990}}");
		PurchasePreview preview = await(corvo().credits().previewPurchase(10));
		assertThat(preview.totalCents()).isEqualTo(990);
		RecordedRequest r = takeRequest();
		assertThat(r.getMethod()).isEqualTo("GET");
		assertThat(r.getRequestUrl().queryParameter("units")).isEqualTo("10");
	}

	@Test
	void webhooksCarryTheEnvironmentAndTestResult() throws Exception {
		enqueueJson(200, "{\"data\":[{\"id\":\"w1\",\"environment\":\"live\",\"events\":[\"nfse.invoice.taker_rejected\"]}],"
				+ "\"pagination\":{\"limit\":50,\"hasMore\":false,\"nextCursor\":null}}");
		enqueueJson(200, "{\"data\":{\"sent\":false,\"error\":\"timeout\"}}");
		assertThat(await(corvo().webhooks().list(null, null)).data().get(0).environment()).isEqualTo("live");
		WebhookTestResult test = await(corvo().webhooks().test("w1"));
		assertThat(test.sent()).isFalse();
		assertThat(test.error()).isEqualTo("timeout");
	}
}
