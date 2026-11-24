package technology.corvo.nfse.resources;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.Paginated;
import technology.corvo.nfse.types.PlanPrice;
import technology.corvo.nfse.types.PlanRecommendation;

/** The public price catalog. These endpoints need no API key. */
public final class PlansResource {

	private static final String BASE = "/plans";

	private final HttpTransport http;

	public PlansResource(HttpTransport http) {
		this.http = http;
	}

	/**
	 * Lists one page of the paid plans and their prices, from the smallest to the largest allowance.
	 * {@code cycle} is {@code monthly} (default when {@code null}) or {@code annual} (discounted).
	 */
	public CompletableFuture<Result<Paginated<PlanPrice>>> list(Integer limit, String cursor, String cycle) {
		Map<String, Object> query = CompaniesResource.listQuery(limit, cursor);
		query.put("cycle", cycle);
		return http.list(BASE, query, UnwrapMode.NONE, PlanPrice.class);
	}

	/** Same as {@link #list(Integer, String, String)} with the monthly cycle. */
	public CompletableFuture<Result<Paginated<PlanPrice>>> list(Integer limit, String cursor) {
		return list(limit, cursor, null);
	}

	/**
	 * The price of one plan: {@code solo}, {@code equipe}, {@code empresa} or {@code escala}.
	 * {@code cycle} is {@code monthly} (default when {@code null}) or {@code annual}.
	 */
	public CompletableFuture<Result<PlanPrice>> get(String slug, String cycle) {
		Map<String, Object> query = new LinkedHashMap<>();
		query.put("cycle", cycle);
		return http.request(
				HttpMethod.GET, BASE + "/" + CompaniesResource.encode(slug), null, query, null, UnwrapMode.DATA,
				PlanPrice.class);
	}

	/** Same as {@link #get(String, String)} with the monthly cycle. */
	public CompletableFuture<Result<PlanPrice>> get(String slug) {
		return get(slug, null);
	}

	/**
	 * The plan recommended for {@code volume} invoices per month. The value is {@code null} (with
	 * no error) when the volume is missing or not positive.
	 */
	public CompletableFuture<Result<PlanRecommendation>> quote(int volume) {
		Map<String, Object> query = new LinkedHashMap<>();
		query.put("volume", volume);
		return http.request(
				HttpMethod.GET, BASE + "/quote", null, query, null, UnwrapMode.DATA, PlanRecommendation.class);
	}
}
