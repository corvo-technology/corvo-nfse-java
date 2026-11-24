package technology.corvo.nfse.resources;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.CreateServiceTaxProfile;
import technology.corvo.nfse.types.ServiceTaxProfile;
import technology.corvo.nfse.types.UpdateServiceTaxProfile;

/** Service tax profiles (the tax classification of each service) of a company. */
public final class ServiceTaxProfilesResource {

	private final HttpTransport http;

	public ServiceTaxProfilesResource(HttpTransport http) {
		this.http = http;
	}

	/** Lists the profiles effective on {@code date} ({@code YYYY-MM-DD}; {@code null} means today). */
	public CompletableFuture<Result<List<ServiceTaxProfile>>> list(String companyId, String date) {
		return http.requestList(HttpMethod.GET, base(companyId), null, Query.of("date", date),
				ServiceTaxProfile.class);
	}

	/** Creates a service tax profile in the company catalog. */
	public CompletableFuture<Result<ServiceTaxProfile>> create(String companyId, CreateServiceTaxProfile params) {
		return http.request(HttpMethod.POST, base(companyId), params, null, null, UnwrapMode.DATA,
				ServiceTaxProfile.class);
	}

	/** Updates a profile; {@code null} fields are not sent. */
	public CompletableFuture<Result<ServiceTaxProfile>> update(
			String companyId, String serviceId, UpdateServiceTaxProfile params) {
		return http.request(HttpMethod.PATCH, item(companyId, serviceId), params, null, null, UnwrapMode.DATA,
				ServiceTaxProfile.class);
	}

	/** Updates a profile with a raw patch that keeps {@code null} values (they clear the field). */
	public CompletableFuture<Result<ServiceTaxProfile>> update(
			String companyId, String serviceId, Map<String, Object> patch) {
		return http.request(HttpMethod.PATCH, item(companyId, serviceId), patch, null, null, UnwrapMode.DATA,
				ServiceTaxProfile.class);
	}

	/** Deletes an expired, not yet approved profile. */
	public CompletableFuture<Result<Void>> remove(String companyId, String serviceId) {
		return http.request(HttpMethod.DELETE, item(companyId, serviceId), null, null, null, UnwrapMode.DATA,
				Void.class);
	}

	/** Approves the saved classification for this company. */
	public CompletableFuture<Result<ServiceTaxProfile>> approve(String companyId, String serviceId) {
		return http.request(HttpMethod.POST, item(companyId, serviceId) + "/approve", null, null, null,
				UnwrapMode.DATA, ServiceTaxProfile.class);
	}

	private static String base(String companyId) {
		return "/companies/" + CompaniesResource.encode(companyId) + "/service-tax-profiles";
	}

	private static String item(String companyId, String serviceId) {
		return base(companyId) + "/" + CompaniesResource.encode(serviceId);
	}
}
