package technology.corvo.nfse.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import technology.corvo.nfse.transport.DownloadedFile;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.Invoice;
import technology.corvo.nfse.types.InvoiceBatchItem;
import technology.corvo.nfse.types.InvoiceBatchSummary;
import technology.corvo.nfse.types.InvoiceCalculation;
import technology.corvo.nfse.types.InvoiceCancelParams;
import technology.corvo.nfse.types.InvoiceCreateParams;
import technology.corvo.nfse.types.InvoiceCreateResult;
import technology.corvo.nfse.types.InvoiceOverview;
import technology.corvo.nfse.types.InvoiceReviewResult;
import technology.corvo.nfse.types.RequestInvoiceEmail;
import technology.corvo.nfse.types.ReviewInvoices;
import technology.corvo.nfse.types.Schedule;
import technology.corvo.nfse.types.SubstituteInvoice;
import technology.corvo.nfse.types.InvoiceDownloadUrl;
import technology.corvo.nfse.types.InvoiceEmailResult;
import technology.corvo.nfse.types.InvoiceEvent;
import technology.corvo.nfse.types.Paginated;

/** NFS-e invoice operations. */
public final class InvoicesResource {

	private static final String BASE = "/invoices";

	private final HttpTransport http;

	public InvoicesResource(HttpTransport http) {
		this.http = http;
	}

	/**
	 * Issues an invoice, or schedules one. Safe to retry with the same {@code idempotencyKey} without
	 * double-issuing. With {@code options.type} {@code IMMEDIATE} (default) the answer is {@code 202}
	 * with the invoice queued; with {@code SCHEDULE} or {@code RECURRING} it is {@code 202} with the
	 * schedule (see {@link InvoiceCreateResult#isSchedule()}).
	 */
	public CompletableFuture<Result<InvoiceCreateResult>> create(InvoiceCreateParams params, String idempotencyKey) {
		boolean scheduled =
				params != null
						&& params.options() != null
						&& ("SCHEDULE".equals(params.options().type()) || "RECURRING".equals(params.options().type()));
		return http.request(
						HttpMethod.POST, BASE, params, null, idempotencyKey, UnwrapMode.DATA, JsonNode.class)
				.thenApply(
						res -> {
							if (res instanceof Result.Err<JsonNode> err) {
								return Result.<InvoiceCreateResult>err(err.error());
							}
							JsonNode data = ((Result.Ok<JsonNode>) res).data();
							if (data == null) {
								return Result.<InvoiceCreateResult>ok(null);
							}
							boolean isSchedule = scheduled || data.has("next_run_on") || data.has("anchor_on");
							if (isSchedule) {
								return Result.ok(
										new InvoiceCreateResult(null, http.mapper().convertValue(data, Schedule.class)));
							}
							return Result.ok(
									new InvoiceCreateResult(http.mapper().convertValue(data, Invoice.class), null));
						});
	}

	/** Convenience overload without an idempotency key. */
	public CompletableFuture<Result<InvoiceCreateResult>> create(InvoiceCreateParams params) {
		return create(params, null);
	}

	/**
	 * Issues 1 to 50 invoices in one call, for one company or several. Items are checked one by one:
	 * valid ones are saved together, invalid ones come back {@code rejected} and are not saved. Match
	 * {@code results[].index} to the item you sent, fix only the rejected ones and send them again. Set
	 * an {@code idempotencyKey} per item; the {@code Idempotency-Key} header is not used by this route.
	 */
	public CompletableFuture<Result<InvoiceBatchSummary>> createBatch(List<InvoiceBatchItem> items) {
		List<Map<String, Object>> invoices = new java.util.ArrayList<>();
		for (InvoiceBatchItem item : items) {
			Map<String, Object> entry =
					http.mapper().convertValue(item.invoice(), new TypeReference<LinkedHashMap<String, Object>>() {});
			if (item.idempotencyKey() != null) {
				entry.put("idempotencyKey", item.idempotencyKey());
			}
			invoices.add(entry);
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("invoices", invoices);
		return http.request(
				HttpMethod.POST, BASE + "/batches", body, null, null, UnwrapMode.DATA,
				InvoiceBatchSummary.class);
	}

	/**
	 * Estimates the taxes of an invoice without issuing, storing or charging anything. A fiscal gap
	 * is not an error: the result has {@code status == "blocked"} with {@code blocking} and
	 * {@code pendingFields}.
	 */
	public CompletableFuture<Result<InvoiceCalculation>> calculate(InvoiceCreateParams params) {
		return http.request(
				HttpMethod.POST, BASE + "/calculate", params, null, null, UnwrapMode.DATA,
				InvoiceCalculation.class);
	}

	/** Fetches a single invoice by id. */
	public CompletableFuture<Result<Invoice>> get(String id) {
		return http.request(
				HttpMethod.GET, BASE + "/" + encode(id), null, null, null, UnwrapMode.DATA, Invoice.class);
	}

	/** Lists one page of invoices. */
	public CompletableFuture<Result<Paginated<Invoice>>> list(
			String companyId, String status, String from, String to, String externalId, Integer limit,
			String cursor) {
		return http.list(BASE, listQuery(companyId, status, from, to, externalId, limit, cursor),
				UnwrapMode.NONE, Invoice.class);
	}

	/** Lists one page of invoices, also filtered by the customer's CPF or CNPJ (digits). */
	public CompletableFuture<Result<Paginated<Invoice>>> list(
			String companyId, String status, String from, String to, String externalId, String customerTaxId,
			Integer limit, String cursor) {
		Map<String, Object> query = listQuery(companyId, status, from, to, externalId, limit, cursor);
		query.put("customerTaxId", customerTaxId);
		return http.list(BASE, query, UnwrapMode.NONE, Invoice.class);
	}

	/** Lists one page of invoices with no filters. */
	public CompletableFuture<Result<Paginated<Invoice>>> list() {
		return list(null, null, null, null, null, null, null);
	}

	/**
	 * Walks every page of invoices lazily.
	 *
	 * <p><b>Documented exception to the never-throw rule:</b> if fetching a page fails, a
	 * {@code CorvoApiError} is thrown from the stream.
	 */
	public Stream<Invoice> iterate(
			String companyId, String status, String from, String to, String externalId, Integer limit,
			String cursor) {
		return http.iterate(BASE, listQuery(companyId, status, from, to, externalId, limit, cursor),
				UnwrapMode.NONE, Invoice.class);
	}

	/** Walks every page of invoices of one customer lazily (same exception to the never-throw rule). */
	public Stream<Invoice> iterate(
			String companyId, String status, String from, String to, String externalId, String customerTaxId,
			Integer limit, String cursor) {
		Map<String, Object> query = listQuery(companyId, status, from, to, externalId, limit, cursor);
		query.put("customerTaxId", customerTaxId);
		return http.iterate(BASE, query, UnwrapMode.NONE, Invoice.class);
	}

	/** Lists one page of an invoice's status-transition events. */
	public CompletableFuture<Result<Paginated<InvoiceEvent>>> events(String id, Integer limit, String cursor) {
		return http.list(BASE + "/" + encode(id) + "/events", CompaniesResource.listQuery(limit, cursor),
				UnwrapMode.NONE, InvoiceEvent.class);
	}

	/** Downloads the invoice PDF (binary). */
	public CompletableFuture<Result<DownloadedFile>> pdf(String id) {
		return http.requestRaw(HttpMethod.GET, BASE + "/" + encode(id) + "/pdf", null);
	}

	/** Downloads the invoice XML (binary). */
	public CompletableFuture<Result<DownloadedFile>> xml(String id) {
		return http.requestRaw(HttpMethod.GET, BASE + "/" + encode(id) + "/xml", null);
	}

	/** Requests a presigned download URL ({@code format} is {@code "pdf"} or {@code "xml"}). */
	public CompletableFuture<Result<InvoiceDownloadUrl>> downloadUrl(String id, String format) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("format", format);
		return http.request(
				HttpMethod.POST, BASE + "/" + encode(id) + "/download-url", body, null, null,
				UnwrapMode.DATA, InvoiceDownloadUrl.class);
	}

	/**
	 * Cancels an invoice. A queued invoice is cancelled at once ({@code status = "cancelled"}); an
	 * authorized one moves to {@code cancelling} while the cancellation is processed, and a processing
	 * one stays {@code processing} with {@code cancellationRequestedAt} set. Call {@link #get} later:
	 * {@code cancelled} when done, or back to {@code authorized} with {@code lastErrorCode} when the
	 * cancellation was rejected.
	 */
	public CompletableFuture<Result<Invoice>> cancel(String id, InvoiceCancelParams params) {
		return http.request(
				HttpMethod.POST, BASE + "/" + encode(id) + "/cancel", params, null, null, UnwrapMode.DATA,
				Invoice.class);
	}

	/**
	 * Sends the invoice by email. {@code email} is another destination than the customer's address
	 * ({@code null} uses the customer's). The invoice must be authorized with its documents ready.
	 * Idempotent when {@code idempotencyKey} is set.
	 */
	public CompletableFuture<Result<InvoiceEmailResult>> sendEmail(String id, String email, String idempotencyKey) {
		return http.request(
				HttpMethod.POST, BASE + "/" + encode(id) + "/email", email == null ? null : new RequestInvoiceEmail(email), null,
				idempotencyKey, UnwrapMode.DATA, InvoiceEmailResult.class);
	}

	/** Sends the invoice to the customer by email. Idempotent when {@code idempotencyKey} is set. */
	public CompletableFuture<Result<InvoiceEmailResult>> sendEmail(String id, String idempotencyKey) {
		return sendEmail(id, null, idempotencyKey);
	}

	/** Convenience overload without an idempotency key. */
	public CompletableFuture<Result<InvoiceEmailResult>> sendEmail(String id) {
		return sendEmail(id, null, null);
	}

	/**
	 * Replaces an authorized invoice with a corrected one: only {@code description} (why), {@code
	 * service.description}, {@code purchaseOrder} and {@code notes} can change. Answers {@code 202} with
	 * the new invoice. Idempotent when {@code idempotencyKey} is set.
	 */
	public CompletableFuture<Result<Invoice>> substitute(
			String id, SubstituteInvoice params, String idempotencyKey) {
		return http.request(
				HttpMethod.POST, BASE + "/" + encode(id) + "/substitute", params, null, idempotencyKey,
				UnwrapMode.DATA, Invoice.class);
	}

	/** Convenience overload without an idempotency key. */
	public CompletableFuture<Result<Invoice>> substitute(String id, SubstituteInvoice params) {
		return substitute(id, params, null);
	}

	/** Counters, daily activity and the latest invoices. {@code period} is {@code 7d} (default) or {@code 30d}. */
	public CompletableFuture<Result<InvoiceOverview>> overview(String period) {
		return http.request(
				HttpMethod.GET, BASE + "/overview", null, Query.of("period", period), null, UnwrapMode.DATA,
				InvoiceOverview.class);
	}

	/**
	 * Resolves invoices in {@code manual_review} (up to 100). {@code action} is {@code retry} (send again
	 * with the same DPS, never issues twice) or {@code discard} (reject, only after the portal confirms
	 * the DPS was never registered).
	 */
	public CompletableFuture<Result<InvoiceReviewResult>> review(String action, List<String> invoiceIds) {
		return http.request(
				HttpMethod.POST, BASE + "/review", new ReviewInvoices(action, invoiceIds), null, null,
				UnwrapMode.DATA, InvoiceReviewResult.class);
	}

	private static Map<String, Object> listQuery(
			String companyId, String status, String from, String to, String externalId, Integer limit,
			String cursor) {
		Map<String, Object> query = new LinkedHashMap<>();
		query.put("companyId", companyId);
		query.put("status", status);
		query.put("from", from);
		query.put("to", to);
		query.put("externalId", externalId);
		query.put("limit", limit);
		query.put("cursor", cursor);
		return query;
	}

	private static String encode(String value) {
		return CompaniesResource.encode(value);
	}
}
