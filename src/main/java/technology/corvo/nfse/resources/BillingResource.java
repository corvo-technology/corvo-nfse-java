package technology.corvo.nfse.resources;

import java.util.concurrent.CompletableFuture;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.BillingPlanResponse;
import technology.corvo.nfse.types.QuotaStatus;

/** Billing plan and usage operations. */
public final class BillingResource {

	private final HttpTransport http;

	public BillingResource(HttpTransport http) {
		this.http = http;
	}

	/** Returns the current plan, subscription, and usage snapshot. */
	public CompletableFuture<Result<BillingPlanResponse>> plan() {
		return http.request(
				HttpMethod.GET, "/billing/plan", null, null, null, UnwrapMode.DATA, BillingPlanResponse.class);
	}

	/** Returns the current emission quota. */
	public CompletableFuture<Result<QuotaStatus>> usage() {
		return http.request(HttpMethod.GET, "/billing/usage", null, null, null, UnwrapMode.DATA,
				QuotaStatus.class);
	}
}
