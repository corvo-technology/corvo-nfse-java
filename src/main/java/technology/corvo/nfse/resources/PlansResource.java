package technology.corvo.nfse.resources;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.Paginated;
import technology.corvo.nfse.types.PlanQuote;
import technology.corvo.nfse.types.PlanRecommendation;

/** The public price catalog (monthly cycle only). */
public final class PlansResource {

	private static final String BASE = "/plans";

	private final HttpTransport http;

	public PlansResource(HttpTransport http) {
		this.http = http;
	}

	/** Lists one page of plan prices, from the smallest to the largest allowance. */
	public CompletableFuture<Result<Paginated<PlanQuote>>> list(Integer limit, String cursor) {
		return http.list(BASE, CompaniesResource.listQuery(limit, cursor), UnwrapMode.NONE, PlanQuote.class);
	}

	/** The price of one plan: {@code free}, {@code developer}, {@code growth}, {@code scale} or {@code volume}. */
	public CompletableFuture<Result<PlanQuote>> get(String slug) {
		return http.request(
				HttpMethod.GET, BASE + "/" + CompaniesResource.encode(slug), null, null, null, UnwrapMode.DATA,
				PlanQuote.class);
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
