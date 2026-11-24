package technology.corvo.nfse.resources;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.AccountantSend;
import technology.corvo.nfse.types.AccountantSendResult;
import technology.corvo.nfse.types.AccountantSettings;
import technology.corvo.nfse.types.AccountantSettingsUpdate;
import technology.corvo.nfse.types.SendAccountantPackage;

/** The monthly package (XML, PDF and summary) sent to a company's accountant. */
public final class AccountantPackageResource {

	private final HttpTransport http;

	public AccountantPackageResource(HttpTransport http) {
		this.http = http;
	}

	/** Returns the accountant e-mail of the monthly package. */
	public CompletableFuture<Result<AccountantSettings>> get(String companyId) {
		return http.request(HttpMethod.GET, base(companyId), null, null, null, UnwrapMode.DATA,
				AccountantSettings.class);
	}

	/**
	 * Sets the accountant e-mail. {@code null} (or blank) turns the monthly send off: it is sent as an
	 * explicit {@code null}.
	 */
	public CompletableFuture<Result<AccountantSettingsUpdate>> update(String companyId, String accountantEmail) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("accountantEmail", accountantEmail);
		return http.request(HttpMethod.PUT, base(companyId), body, null, null, UnwrapMode.DATA,
				AccountantSettingsUpdate.class);
	}

	/** Sends the package of a month ({@code YYYY-MM}) to the accountant now. */
	public CompletableFuture<Result<AccountantSendResult>> send(String companyId, String month) {
		return http.request(HttpMethod.POST, base(companyId) + "/send", new SendAccountantPackage(month), null,
				null, UnwrapMode.DATA, AccountantSendResult.class);
	}

	/** Lists the last 12 attempts to send the package to the accountant. */
	public CompletableFuture<Result<List<AccountantSend>>> listSends(String companyId) {
		return http.requestList(HttpMethod.GET, base(companyId) + "/sends", null, null, AccountantSend.class);
	}

	private static String base(String companyId) {
		return "/companies/" + CompaniesResource.encode(companyId) + "/accountant-package";
	}
}
