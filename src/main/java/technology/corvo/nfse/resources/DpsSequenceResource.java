package technology.corvo.nfse.resources;

import java.util.concurrent.CompletableFuture;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.DpsSequence;
import technology.corvo.nfse.types.SetDpsSequence;

/** DPS numbering of a company. */
public final class DpsSequenceResource {

	private final HttpTransport http;

	public DpsSequenceResource(HttpTransport http) {
		this.http = http;
	}

	/** Returns the DPS numbering of a company. */
	public CompletableFuture<Result<DpsSequence>> get(String companyId) {
		return http.request(HttpMethod.GET, path(companyId), null, null, null, UnwrapMode.DATA, DpsSequence.class);
	}

	/**
	 * Continues the numbering from the last note issued elsewhere: the next note uses {@code
	 * lastNumber + 1}. Answers {@code DPS_NUMBER_BELOW_CURRENT} for a number below the current one.
	 */
	public CompletableFuture<Result<DpsSequence>> set(String companyId, SetDpsSequence params) {
		return http.request(HttpMethod.PUT, path(companyId), params, null, null, UnwrapMode.DATA,
				DpsSequence.class);
	}

	/** Same as {@link #set(String, SetDpsSequence)} for a whole number. */
	public CompletableFuture<Result<DpsSequence>> set(String companyId, long lastNumber) {
		return set(companyId, new SetDpsSequence((double) lastNumber));
	}

	private static String path(String companyId) {
		return "/companies/" + CompaniesResource.encode(companyId) + "/dps-sequence";
	}
}
