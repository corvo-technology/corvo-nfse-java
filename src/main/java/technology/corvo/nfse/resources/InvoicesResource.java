package technology.corvo.nfse.resources;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import technology.corvo.nfse.transport.DownloadedFile;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.Invoice;
import technology.corvo.nfse.types.InvoiceCreateParams;
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
	 * Issues an invoice. Safe to retry with the same {@code idempotencyKey} without double-issuing.
	 */
	public CompletableFuture<Result<Invoice>> create(InvoiceCreateParams params, String idempotencyKey) {
		return http.request(
				HttpMethod.POST, BASE, params, null, idempotencyKey, UnwrapMode.DATA_PROPS, Invoice.class);
	}

	/** Convenience overload without an idempotency key. */
	public CompletableFuture<Result<Invoice>> create(InvoiceCreateParams params) {
		return create(params, null);
	}

	/** Fetches a single invoice by id. */
	public CompletableFuture<Result<Invoice>> get(String id) {
		return http.request(
				HttpMethod.GET, BASE + "/" + encode(id), null, null, null, UnwrapMode.DATA_PROPS, Invoice.class);
	}

	/** Lists one page of invoices. */
	public CompletableFuture<Result<Paginated<Invoice>>> list(
			String companyId, String status, String from, String to, String externalId, Integer limit,
			String cursor) {
		return http.list(BASE, listQuery(companyId, status, from, to, externalId, limit, cursor),
				UnwrapMode.DATA_PROPS, Invoice.class);
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
				UnwrapMode.DATA_PROPS, Invoice.class);
	}

	/** Lists one page of an invoice's status-transition events. */
	public CompletableFuture<Result<Paginated<InvoiceEvent>>> events(String id, Integer limit, String cursor) {
		return http.list(BASE + "/" + encode(id) + "/events", CompaniesResource.listQuery(limit, cursor),
				UnwrapMode.DATA_PROPS, InvoiceEvent.class);
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

	/** Sends the invoice to the customer by email. Idempotent when {@code idempotencyKey} is set. */
	public CompletableFuture<Result<InvoiceEmailResult>> sendEmail(String id, String idempotencyKey) {
		return http.request(
				HttpMethod.POST, BASE + "/" + encode(id) + "/email", null, null, idempotencyKey,
				UnwrapMode.DATA, InvoiceEmailResult.class);
	}

	/** Convenience overload without an idempotency key. */
	public CompletableFuture<Result<InvoiceEmailResult>> sendEmail(String id) {
		return sendEmail(id, null);
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
