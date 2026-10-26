package technology.corvo.nfse.resources;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import technology.corvo.nfse.Corvo;
import technology.corvo.nfse.TestHarness;
import technology.corvo.nfse.transport.Result;
import java.util.List;
import technology.corvo.nfse.types.Invoice;
import technology.corvo.nfse.types.InvoiceBatchItem;
import technology.corvo.nfse.types.InvoiceBatchSummary;
import technology.corvo.nfse.types.InvoiceCancelParams;
import technology.corvo.nfse.types.InvoiceCreateParams;
import technology.corvo.nfse.types.InvoiceEvent;
import technology.corvo.nfse.types.Paginated;

class InvoicesResourceTest extends TestHarness {

	private static final String INVOICE_JSON =
			"{\"id\":\"inv_1\",\"companyId\":\"comp_1\",\"tenantId\":\"t_1\",\"environment\":\"test\","
					+ "\"documentKeyVersion\":1,\"emailSnapshot\":{\"send\":true},\"externalId\":\"order-42\","
					+ "\"idempotencyKey\":null,\"status\":\"authorized\","
					+ "\"customerSnapshot\":{\"name\":\"Customer\",\"email\":\"a@b.com\",\"taxId\":\"52998224725\","
					+ "\"address\":null},"
					+ "\"serviceSnapshot\":{\"code\":\"010101\",\"description\":\"Services\","
					+ "\"municipalityCode\":\"3170206\"},"
					+ "\"taxSnapshot\":{},\"amount\":150.5,\"currency\":\"BRL\","
					+ "\"competenceDate\":\"2026-10-12\",\"dpsId\":null,\"dpsSeries\":null,\"dpsNumber\":null,"
					+ "\"accessKey\":\"3526\",\"nfseNumber\":7,\"fiscalProvider\":\"gov\","
					+ "\"fiscalSchemaVersion\":\"1.00\",\"issuedAt\":null,\"authorizedAt\":null,"
					+ "\"lastErrorCode\":null,\"lastErrorMessage\":null,\"lastErrorProvider\":null,"
					+ "\"documentsReadyAt\":null,\"lastEmailStatus\":null,\"lastEmailSentAt\":null,"
					+ "\"createdAt\":\"2026-10-12T00:00:00Z\",\"updatedAt\":\"2026-10-12T00:00:00Z\","
					+ "\"documentsStatus\":\"pending\",\"emailStatus\":\"pending\",\"stage\":\"authorized\"}";

	@Test
	void createReturnsTheInvoiceAndSendsIdempotencyKey() throws Exception {
		enqueueJson(200, "{\"data\":" + INVOICE_JSON + "}");

		InvoiceCreateParams params =
				InvoiceCreateParams.of(
								InvoiceCreateParams.Customer.of("Customer", "52998224725").withEmail("a@b.com"),
								InvoiceCreateParams.Service.of("010101", "3170206")
										.withDescription("Services"),
								150.5,
								"2026-10-12")
						.withCompanyId("comp_1")
						.withExternalId("order-42");

		Result<Invoice> result =
				corvo().invoices().create(params, "order-42").get(5, TimeUnit.SECONDS);

		Invoice invoice = ((Result.Ok<Invoice>) result).data();
		assertThat(invoice.id()).isEqualTo("inv_1");
		assertThat(invoice.nfseNumber()).isEqualTo(7);
		assertThat(invoice.customerSnapshot().name()).isEqualTo("Customer");
		RecordedRequest request = takeRequest();
		assertThat(request.getPath()).isEqualTo("/v1/invoices");
		assertThat(request.getHeader("Idempotency-Key")).isEqualTo("order-42");
		// Wire casing pass-through: camelCase fields serialized as-is
		assertThat(request.getBody().readUtf8())
				.contains("\"competenceDate\":\"2026-10-12\"")
				.contains("\"amount\":150.5");
	}

	@Test
	void createBatchPostsTheItemsWithoutAnIdempotencyKeyHeader() throws Exception {
		enqueueJson(
				202,
				"{\"data\":{\"total\":2,\"accepted\":1,\"rejected\":1,\"results\":["
						+ "{\"index\":0,\"status\":\"accepted\",\"replayed\":true,\"invoice\":" + INVOICE_JSON + "},"
						+ "{\"index\":1,\"status\":\"rejected\",\"error\":{\"code\":\"ERROR_VALIDATION\","
						+ "\"message\":\"Request validation failed\"}}]}}");

		InvoiceCreateParams params =
				InvoiceCreateParams.of(
								null, InvoiceCreateParams.Service.ofDescription("Consultoria"), 150.5, "2026-10-01")
						.withCompanyTaxId("11222333000181")
						.withDeduction(20);

		Result<InvoiceBatchSummary> result =
				corvo().invoices().createBatch(List.of(InvoiceBatchItem.of(params, "sheet-row-2")))
						.get(5, TimeUnit.SECONDS);

		InvoiceBatchSummary summary = ((Result.Ok<InvoiceBatchSummary>) result).data();
		assertThat(summary.total()).isEqualTo(2);
		assertThat(summary.results().get(0).replayed()).isTrue();
		assertThat(summary.results().get(0).invoice().id()).isEqualTo("inv_1");
		assertThat(summary.results().get(1).error().code()).isEqualTo("ERROR_VALIDATION");
		RecordedRequest request = takeRequest();
		assertThat(request.getPath()).isEqualTo("/v1/invoices/batches");
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getHeader("Idempotency-Key")).isNull();
		assertThat(request.getBody().readUtf8())
				.contains("\"invoices\":[{")
				.contains("\"idempotencyKey\":\"sheet-row-2\"")
				.contains("\"deduction\":20.0")
				.contains("\"companyTaxId\":\"11222333000181\"");
	}

	@Test
	void cancelPostsReasonAndDescription() throws Exception {
		enqueueJson(
				202,
				"{\"data\":" + INVOICE_JSON.replace("\"status\":\"authorized\"", "\"status\":\"cancelling\"")
						+ "}");

		Result<Invoice> result =
				corvo().invoices()
						.cancel("inv_1", InvoiceCancelParams.of("issue_error", "Wrong amount on the invoice"))
						.get(5, TimeUnit.SECONDS);

		assertThat(((Result.Ok<Invoice>) result).data().status()).isEqualTo("cancelling");
		RecordedRequest request = takeRequest();
		assertThat(request.getPath()).isEqualTo("/v1/invoices/inv_1/cancel");
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getBody().readUtf8())
				.contains("\"reason\":\"issue_error\"")
				.contains("\"description\":\"Wrong amount on the invoice\"");
	}

	@Test
	void getReturnsTheInvoice() throws Exception {
		enqueueJson(200, "{\"data\":" + INVOICE_JSON + "}");

		Result<Invoice> result = corvo().invoices().get("inv_1").get(5, TimeUnit.SECONDS);

		assertThat(((Result.Ok<Invoice>) result).data().status()).isEqualTo("authorized");
		assertThat(takeRequest().getPath()).isEqualTo("/v1/invoices/inv_1");
	}

	@Test
	void listSendsFilters() throws Exception {
		enqueueJson(
				200,
				"{\"data\":[" + INVOICE_JSON + "],"
						+ "\"pagination\":{\"limit\":10,\"hasMore\":false,\"nextCursor\":null}}");

		corvo().invoices().list("comp_1", "authorized", "2026-10-01", "2026-10-31", null, 10, null)
				.get(5, TimeUnit.SECONDS);

		RecordedRequest request = takeRequest();
		assertThat(request.getRequestUrl().queryParameter("companyId")).isEqualTo("comp_1");
		assertThat(request.getRequestUrl().queryParameter("status")).isEqualTo("authorized");
		assertThat(request.getRequestUrl().queryParameter("from")).isEqualTo("2026-10-01");
		assertThat(request.getRequestUrl().queryParameter("to")).isEqualTo("2026-10-31");
	}

	@Test
	void eventsReturnsTheItems() throws Exception {
		enqueueJson(
				200,
				"{\"data\":[{\"id\":\"evt_1\",\"invoiceId\":\"inv_1\","
						+ "\"fromStatus\":null,\"toStatus\":\"authorized\",\"reason\":null,"
						+ "\"createdAt\":\"2026-10-12T00:00:00Z\"}],"
						+ "\"pagination\":{\"limit\":50,\"hasMore\":false,\"nextCursor\":null}}");

		Result<Paginated<InvoiceEvent>> result =
				corvo().invoices().events("inv_1", null, null).get(5, TimeUnit.SECONDS);

		InvoiceEvent event = ((Result.Ok<Paginated<InvoiceEvent>>) result).data().data().get(0);
		assertThat(event.toStatus()).isEqualTo("authorized");
		assertThat(takeRequest().getPath()).startsWith("/v1/invoices/inv_1/events");
	}

	@Test
	void pdfAndXmlReturnBinaryFiles() throws Exception {
		byte[] pdf = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);
		byte[] xml = "<DPS/>".getBytes(StandardCharsets.UTF_8);
		server.enqueue(
				new MockResponse()
						.setResponseCode(200)
						.setHeader("Content-Type", "application/pdf")
						.setBody(new okio.Buffer().write(pdf)));
		server.enqueue(
				new MockResponse()
						.setResponseCode(200)
						.setHeader("Content-Type", "application/xml")
						.setBody(new okio.Buffer().write(xml)));

		assertThat(
						((technology.corvo.nfse.transport.Result.Ok<
										technology.corvo.nfse.transport.DownloadedFile>)
										corvo().invoices().pdf("inv_1").get(5, TimeUnit.SECONDS))
								.data()
								.content())
				.isEqualTo(pdf);
		assertThat(
						((technology.corvo.nfse.transport.Result.Ok<
										technology.corvo.nfse.transport.DownloadedFile>)
										corvo().invoices().xml("inv_1").get(5, TimeUnit.SECONDS))
								.data()
								.content())
				.isEqualTo(xml);
	}

	@Test
	void sendEmailPostsAndPassesIdempotencyKey() throws Exception {
		enqueueJson(200,
				"{\"data\":{\"emailId\":\"em_1\",\"status\":\"sent\"}}");

		corvo().invoices().sendEmail("inv_1", "mail-1").get(5, TimeUnit.SECONDS);

		RecordedRequest request = takeRequest();
		assertThat(request.getPath()).isEqualTo("/v1/invoices/inv_1/email");
		assertThat(request.getHeader("Idempotency-Key")).isEqualTo("mail-1");
	}

	private Corvo corvo() {
		return Corvo.builder().apiKey(API_KEY).baseUrl(server.url("/").toString()).build();
	}
}
