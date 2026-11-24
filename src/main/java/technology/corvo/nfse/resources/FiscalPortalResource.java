package technology.corvo.nfse.resources;

import java.util.concurrent.CompletableFuture;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.PortalStatus;

/** Status of the national NFS-e portal. */
public final class FiscalPortalResource {

	private final HttpTransport http;

	public FiscalPortalResource(HttpTransport http) {
		this.http = http;
	}

	/** Whether the national portal is answering for this environment. */
	public CompletableFuture<Result<PortalStatus>> status() {
		return http.request(HttpMethod.GET, "/fiscal-portal/status", null, null, null, UnwrapMode.DATA,
				PortalStatus.class);
	}
}
