package technology.corvo.nfse.resources;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.Company;
import technology.corvo.nfse.types.CompanyCreateParams;
import technology.corvo.nfse.types.CompanyUpdateParams;
import technology.corvo.nfse.types.Paginated;

/** Companies (emitters) operations. */
public final class CompaniesResource {

	private static final String BASE = "/companies";

	private final HttpTransport http;

	public CompaniesResource(HttpTransport http) {
		this.http = http;
	}

	/** Creates a company. */
	public CompletableFuture<Result<Company>> create(CompanyCreateParams params) {
		return http.request(HttpMethod.POST, BASE, params, null, null, UnwrapMode.DATA, Company.class);
	}

	/** Fetches a single company by id. */
	public CompletableFuture<Result<Company>> get(String id) {
		return http.request(
				HttpMethod.GET, BASE + "/" + encode(id), null, null, null, UnwrapMode.DATA, Company.class);
	}

	/** Updates a company; only non-null fields of {@code params} are sent. */
	public CompletableFuture<Result<Company>> update(String id, CompanyUpdateParams params) {
		return http.request(
				HttpMethod.PATCH, BASE + "/" + encode(id), params, null, null, UnwrapMode.DATA, Company.class);
	}

	/** Deletes a company. 204 responses yield {@code Ok(null)}. */
	public CompletableFuture<Result<Void>> remove(String id) {
		return http.request(
				HttpMethod.DELETE, BASE + "/" + encode(id), null, null, null, UnwrapMode.DATA, Void.class);
	}

	/** Lists one page of companies. */
	public CompletableFuture<Result<Paginated<Company>>> list(Integer limit, String cursor) {
		return http.list(BASE, listQuery(limit, cursor), UnwrapMode.DATA, Company.class);
	}

	/**
	 * Walks every page of companies lazily.
	 *
	 * <p><b>Documented exception to the never-throw rule:</b> if fetching a page fails, a
	 * {@code CorvoApiError} is thrown from the stream.
	 */
	public Stream<Company> iterate(Integer limit, String cursor) {
		return http.iterate(BASE, listQuery(limit, cursor), UnwrapMode.DATA, Company.class);
	}

	static Map<String, Object> listQuery(Integer limit, String cursor) {
		Map<String, Object> query = new LinkedHashMap<>();
		query.put("limit", limit);
		query.put("cursor", cursor);
		return query;
	}

	static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}
}
