package technology.corvo.nfse.resources;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.Paginated;
import technology.corvo.nfse.types.SavedCustomer;
import technology.corvo.nfse.types.UpdateCustomer;

/** Saved customers (tomadores), keyed by CPF or CNPJ. */
public final class CustomersResource {

	private static final String BASE = "/customers";

	private final HttpTransport http;

	public CustomersResource(HttpTransport http) {
		this.http = http;
	}

	/**
	 * Lists one page of saved customers, most recently used first. {@code search} filters by name or
	 * tax id. The cursor is the offset of the next page: pass it back untouched.
	 */
	public CompletableFuture<Result<Paginated<SavedCustomer>>> search(
			String search, Integer limit, String cursor) {
		return http.list(BASE, Query.of("search", search, "limit", limit, "cursor", cursor), UnwrapMode.NONE,
				SavedCustomer.class);
	}

	/**
	 * Walks every page of saved customers lazily.
	 *
	 * <p><b>Documented exception to the never-throw rule:</b> if fetching a page fails, a
	 * {@code CorvoApiError} is thrown from the stream.
	 */
	public Stream<SavedCustomer> iterate(String search, Integer limit, String cursor) {
		return http.iterate(BASE, Query.of("search", search, "limit", limit, "cursor", cursor),
				UnwrapMode.NONE, SavedCustomer.class);
	}

	/** Fetches a saved customer by CPF or CNPJ. */
	public CompletableFuture<Result<SavedCustomer>> get(String taxId) {
		return http.request(HttpMethod.GET, path(taxId), null, null, null, UnwrapMode.DATA, SavedCustomer.class);
	}

	/** Corrects the name or the email of a saved customer; {@code null} fields are not sent. */
	public CompletableFuture<Result<SavedCustomer>> update(String taxId, UpdateCustomer params) {
		return http.request(HttpMethod.PATCH, path(taxId), params, null, null, UnwrapMode.DATA,
				SavedCustomer.class);
	}

	/**
	 * Same as {@link #update(String, UpdateCustomer)} with a raw patch that keeps {@code null} values:
	 * {@code Map.of("email", null)} is not allowed by {@code Map.of}, so use a {@code HashMap}. Sending
	 * {@code "email": null} removes the saved email.
	 */
	public CompletableFuture<Result<SavedCustomer>> update(String taxId, Map<String, Object> patch) {
		return http.request(HttpMethod.PATCH, path(taxId), patch, null, null, UnwrapMode.DATA,
				SavedCustomer.class);
	}

	/** Forgets a saved customer (issued invoices are not changed). */
	public CompletableFuture<Result<Void>> remove(String taxId) {
		return http.request(HttpMethod.DELETE, path(taxId), null, null, null, UnwrapMode.DATA, Void.class);
	}

	private static String path(String taxId) {
		return BASE + "/" + CompaniesResource.encode(taxId);
	}
}
