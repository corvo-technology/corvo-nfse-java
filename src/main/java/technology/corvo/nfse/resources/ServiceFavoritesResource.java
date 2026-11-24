package technology.corvo.nfse.resources;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.CreateServiceFavorite;
import technology.corvo.nfse.types.Deleted;
import technology.corvo.nfse.types.ServiceFavorite;

/** Favorite services of a company (a saved service code, description and default amount). */
public final class ServiceFavoritesResource {

	private final HttpTransport http;

	public ServiceFavoritesResource(HttpTransport http) {
		this.http = http;
	}

	/** Lists the favorite services of a company. */
	public CompletableFuture<Result<List<ServiceFavorite>>> list(String companyId) {
		return http.requestList(HttpMethod.GET, base(companyId), null, null, ServiceFavorite.class);
	}

	/** Saves a favorite service. */
	public CompletableFuture<Result<ServiceFavorite>> create(String companyId, CreateServiceFavorite params) {
		return http.request(HttpMethod.POST, base(companyId), params, null, null, UnwrapMode.DATA,
				ServiceFavorite.class);
	}

	/** Removes a favorite service. */
	public CompletableFuture<Result<Deleted>> remove(String companyId, String favoriteId) {
		return http.request(
				HttpMethod.DELETE, base(companyId) + "/" + CompaniesResource.encode(favoriteId), null, null,
				null, UnwrapMode.DATA, Deleted.class);
	}

	private static String base(String companyId) {
		return "/companies/" + CompaniesResource.encode(companyId) + "/service-favorites";
	}
}
