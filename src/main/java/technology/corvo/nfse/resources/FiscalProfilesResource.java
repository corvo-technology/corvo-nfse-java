package technology.corvo.nfse.resources;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.FiscalProfile;
import technology.corvo.nfse.types.UpsertFiscalProfile;

/** Fiscal profile (validity periods of the tax settings) of a company. */
public final class FiscalProfilesResource {

	private final HttpTransport http;

	public FiscalProfilesResource(HttpTransport http) {
		this.http = http;
	}

	/** Lists the fiscal profile history of the company. */
	public CompletableFuture<Result<List<FiscalProfile>>> list(String companyId) {
		return http.requestList(HttpMethod.GET, base(companyId), null, null, FiscalProfile.class);
	}

	/** Creates a fiscal profile validity period (idempotent for an identical desired state). */
	public CompletableFuture<Result<FiscalProfile>> upsert(String companyId, UpsertFiscalProfile params) {
		return http.request(HttpMethod.PUT, base(companyId), params, null, null, UnwrapMode.DATA,
				FiscalProfile.class);
	}

	/** Approves a fiscal profile of the company. */
	public CompletableFuture<Result<FiscalProfile>> approve(String companyId, String profileId) {
		return http.request(
				HttpMethod.POST, base(companyId) + "/" + CompaniesResource.encode(profileId) + "/approve", null,
				null, null, UnwrapMode.DATA, FiscalProfile.class);
	}

	private static String base(String companyId) {
		return "/companies/" + CompaniesResource.encode(companyId) + "/fiscal-profile";
	}
}
