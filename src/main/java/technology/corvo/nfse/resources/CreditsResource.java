package technology.corvo.nfse.resources;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.CreditBalance;
import technology.corvo.nfse.types.CreditPlan;
import technology.corvo.nfse.types.CreditTransaction;
import technology.corvo.nfse.types.Paginated;

/** Credit balance, plans, and history operations. */
public final class CreditsResource {

	private static final String BASE = "/credits";

	private final HttpTransport http;

	public CreditsResource(HttpTransport http) {
		this.http = http;
	}

	/** Returns the tenant's credit balance. */
	public CompletableFuture<Result<CreditBalance>> balance() {
		return http.request(HttpMethod.GET, BASE, null, null, null, UnwrapMode.DATA, CreditBalance.class);
	}

	/** Lists one page of purchasable credit plans. */
	public CompletableFuture<Result<Paginated<CreditPlan>>> plans(Integer limit, String cursor) {
		return http.list(BASE + "/plans", CompaniesResource.listQuery(limit, cursor), UnwrapMode.DATA_PROPS,
				CreditPlan.class);
	}

	/** Lists one page of credit ledger transactions. */
	public CompletableFuture<Result<Paginated<CreditTransaction>>> history(Integer limit, String cursor) {
		return http.list(BASE + "/history", CompaniesResource.listQuery(limit, cursor), UnwrapMode.DATA_PROPS,
				CreditTransaction.class);
	}

	/**
	 * Walks every page of the credit ledger lazily.
	 *
	 * <p><b>Documented exception to the never-throw rule:</b> if fetching a page fails, a
	 * {@code CorvoApiError} is thrown from the stream.
	 */
	public Stream<CreditTransaction> iterateHistory(Integer limit, String cursor) {
		return http.iterate(BASE + "/history", CompaniesResource.listQuery(limit, cursor),
				UnwrapMode.DATA_PROPS, CreditTransaction.class);
	}
}
