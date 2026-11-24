package technology.corvo.nfse.resources;

import java.util.concurrent.CompletableFuture;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.CnpjProfile;

/** CNPJ registry lookup. */
public final class CnpjResource {

	private final HttpTransport http;

	public CnpjResource(HttpTransport http) {
		this.http = http;
	}

	/**
	 * Looks up the registration data of a CNPJ (digits or formatted). Answers {@code CNPJ_INVALID} for
	 * a malformed number and {@code CNPJ_LOOKUP_UNAVAILABLE} when the registry is down.
	 */
	public CompletableFuture<Result<CnpjProfile>> lookup(String cnpj) {
		return http.request(
				HttpMethod.GET, "/cnpj/" + CompaniesResource.encode(cnpj), null, null, null, UnwrapMode.DATA,
				CnpjProfile.class);
	}
}
