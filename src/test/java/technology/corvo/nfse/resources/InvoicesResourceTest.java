package technology.corvo.nfse.resources;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import technology.corvo.nfse.Corvo;
import technology.corvo.nfse.TestHarness;
import technology.corvo.nfse.error.CorvoApiError;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.types.CancelInvoiceParams;
import technology.corvo.nfse.types.CancellationReason;
import technology.corvo.nfse.types.Invoice;
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

	/** {@code INVOICE_JSON} with a different status and the cancellation fields appended. */
	private static String invoiceJson(String status, String cancellationFields) {
		return INVOICE_JSON
				.replace("\"status\":\"authorized\"", "\"status\":\"" + status + "\"")
				.replaceFirst("}$", "," + cancellationFields + "}");
	}

	private static final String CANCELLED_FIELDS =
			"\"cancellationReason\":\"service_not_provided\","
					+ "\"cancellationDescription\":\"Service was not provided to the customer\","
					+ "\"cancelledAt\":\"2026-10-20T12:00:00Z\","
					+ "\"cancellationRequestedAt\":\"2026-10-20T11:59:00Z\"";

	@Test
	void cancelPostsReasonAndDescriptionAndReturnsTheCancelledInvoice() throws Exception {
		enqueueJson(200, "{\"data\":" + invoiceJson("cancelled", CANCELLED_FIELDS) + "}");

		Result<Invoice> result =
				corvo().invoices()
						.cancel(
								"inv_1",
								CancelInvoiceParams.of(CancellationReason.SERVICE_NOT_PROVIDED)
										.withDescription("Service was not provided to the customer"))
						.get(5, TimeUnit.SECONDS);

		Invoice invoice = ((Result.Ok<Invoice>) result).data();
		assertThat(invoice.status()).isEqualTo("cancelled");
		assertThat(invoice.cancellationReason()).isEqualTo(CancellationReason.SERVICE_NOT_PROVIDED);
		assertThat(invoice.cancelledAt()).isEqualTo("2026-10-20T12:00:00Z");
		RecordedRequest request = takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).isEqualTo("/v1/invoices/inv_1/cancel");
		assertThat(request.getHeader("X-API-Key")).isEqualTo(API_KEY);
		assertThat(request.getBody().readUtf8())
				.isEqualTo(
						"{\"reason\":\"service_not_provided\","
								+ "\"description\":\"Service was not provided to the customer\"}");
	}

	@Test
	void cancelWithoutDescriptionOmitsTheField() throws Exception {
		enqueueJson(200, "{\"data\":" + invoiceJson("cancelled", CANCELLED_FIELDS) + "}");

		corvo().invoices()
				.cancel("inv/1", CancelInvoiceParams.of(CancellationReason.ISSUE_ERROR))
				.get(5, TimeUnit.SECONDS);

		RecordedRequest request = takeRequest();
		assertThat(request.getPath()).isEqualTo("/v1/invoices/inv%2F1/cancel");
		assertThat(request.getBody().readUtf8()).isEqualTo("{\"reason\":\"issue_error\"}");
	}

	@Test
	void cancelAcceptedReturnsTheCancellingInvoice() throws Exception {
		enqueueJson(
				202,
				"{\"data\":"
						+ invoiceJson(
								"cancelling",
								"\"cancellationReason\":\"other\","
										+ "\"cancellationDescription\":\"Duplicated invoice for order 42\","
										+ "\"cancelledAt\":null,"
										+ "\"cancellationRequestedAt\":\"2026-10-20T11:59:00Z\"")
						+ "}");

		Result<Invoice> result =
				corvo().invoices()
						.cancel(
								"inv_1",
								CancelInvoiceParams.of(CancellationReason.OTHER)
										.withDescription("Duplicated invoice for order 42"))
						.get(5, TimeUnit.SECONDS);

		Invoice invoice = ((Result.Ok<Invoice>) result).data();
		assertThat(invoice.status()).isEqualTo("cancelling");
		assertThat(invoice.cancellationReason()).isEqualTo(CancellationReason.OTHER);
		assertThat(invoice.cancelledAt()).isNull();
		assertThat(invoice.cancellationRequestedAt()).isEqualTo("2026-10-20T11:59:00Z");
	}

	@Test
	void cancelNotCancellableReturnsTheApiError() throws Exception {
		enqueueJson(
				409,
				"{\"statusCode\":409,\"code\":\"ERROR_INVOICE_NOT_CANCELLABLE\","
						+ "\"message\":\"Invoice cannot be cancelled in status manual_review\","
						+ "\"requestId\":\"req_409\"}");

		Result<Invoice> result =
				corvo().invoices()
						.cancel("inv_1", CancelInvoiceParams.of(CancellationReason.ISSUE_ERROR))
						.get(5, TimeUnit.SECONDS);

		assertThat(result).isInstanceOf(Result.Err.class);
		CorvoApiError error = ((Result.Err<Invoice>) result).error();
		assertThat(error.status()).isEqualTo(409);
		assertThat(error.code()).isEqualTo("ERROR_INVOICE_NOT_CANCELLABLE");
		assertThat(error.requestId()).isEqualTo("req_409");
	}

	@Test
	void cancelInvalidDescriptionReturnsTheApiError() throws Exception {
		enqueueJson(
				422,
				"{\"statusCode\":422,\"code\":\"ERROR_VALIDATION\","
						+ "\"message\":\"description must have 15 to 255 characters\"}");

		Result<Invoice> result =
				corvo().invoices()
						.cancel(
								"inv_1",
								CancelInvoiceParams.of(CancellationReason.OTHER).withDescription("short"))
						.get(5, TimeUnit.SECONDS);

		CorvoApiError error = ((Result.Err<Invoice>) result).error();
		assertThat(error.status()).isEqualTo(422);
		assertThat(error.code()).isEqualTo("ERROR_VALIDATION");
	}

	@Test
	void getParsesACancelledInvoice() throws Exception {
		enqueueJson(200, "{\"data\":" + invoiceJson("cancelled", CANCELLED_FIELDS) + "}");

		Invoice invoice =
				((Result.Ok<Invoice>) corvo().invoices().get("inv_1").get(5, TimeUnit.SECONDS)).data();

		assertThat(invoice.status()).isEqualTo("cancelled");
		assertThat(invoice.cancellationReason()).isEqualTo(CancellationReason.SERVICE_NOT_PROVIDED);
		assertThat(invoice.cancellationDescription())
				.isEqualTo("Service was not provided to the customer");
		assertThat(invoice.cancelledAt()).isEqualTo("2026-10-20T12:00:00Z");
		assertThat(invoice.cancellationRequestedAt()).isEqualTo("2026-10-20T11:59:00Z");
	}

	@Test
	void getParsesACancellingInvoice() throws Exception {
		enqueueJson(
				200,
				"{\"data\":"
						+ invoiceJson(
								"cancelling",
								"\"cancellationReason\":\"issue_error\",\"cancellationDescription\":null,"
										+ "\"cancelledAt\":null,"
										+ "\"cancellationRequestedAt\":\"2026-10-20T11:59:00Z\"")
						+ "}");

		Invoice invoice =
				((Result.Ok<Invoice>) corvo().invoices().get("inv_1").get(5, TimeUnit.SECONDS)).data();

		assertThat(invoice.status()).isEqualTo("cancelling");
		assertThat(invoice.cancellationReason()).isEqualTo(CancellationReason.ISSUE_ERROR);
		assertThat(invoice.cancellationDescription()).isNull();
		assertThat(invoice.cancelledAt()).isNull();
		assertThat(invoice.cancellationRequestedAt()).isEqualTo("2026-10-20T11:59:00Z");
	}

	@Test
	void getOfAnInvoiceWithoutCancellationFieldsLeavesThemNull() throws Exception {
		enqueueJson(200, "{\"data\":" + INVOICE_JSON + "}");

		Invoice invoice =
				((Result.Ok<Invoice>) corvo().invoices().get("inv_1").get(5, TimeUnit.SECONDS)).data();

		assertThat(invoice.cancellationReason()).isNull();
		assertThat(invoice.cancellationDescription()).isNull();
		assertThat(invoice.cancelledAt()).isNull();
		assertThat(invoice.cancellationRequestedAt()).isNull();
	}

	@Test
	void getToleratesAnUnknownCancellationReason() throws Exception {
		enqueueJson(
				200,
				"{\"data\":"
						+ invoiceJson("cancelled", "\"cancellationReason\":\"some_future_reason\"")
						+ "}");

		Result<Invoice> result = corvo().invoices().get("inv_1").get(5, TimeUnit.SECONDS);

		Invoice invoice = ((Result.Ok<Invoice>) result).data();
		assertThat(invoice.status()).isEqualTo("cancelled");
		assertThat(invoice.cancellationReason()).isNull();
	}

	private Corvo corvo() {
		return Corvo.builder().apiKey(API_KEY).baseUrl(server.url("/").toString()).build();
	}
}
