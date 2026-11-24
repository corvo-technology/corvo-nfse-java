package technology.corvo.nfse.resources;

import java.util.concurrent.CompletableFuture;
import technology.corvo.nfse.transport.DownloadedFile;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.PackageSummary;

/** The monthly invoice package of a company (a ZIP with {@code xml/}, {@code pdf/} and {@code resumo.csv}). */
public final class InvoicePackageResource {

	private final HttpTransport http;

	public InvoicePackageResource(HttpTransport http) {
		this.http = http;
	}

	/** Count and total of the package of a month ({@code YYYY-MM}), without building it. */
	public CompletableFuture<Result<PackageSummary>> summary(String companyId, String month) {
		return http.request(HttpMethod.GET, base(companyId) + "/summary", null, Query.of("month", month), null,
				UnwrapMode.DATA, PackageSummary.class);
	}

	/**
	 * Downloads the ZIP of a month ({@code YYYY-MM}), at most 500 notes (else {@code PACKAGE_TOO_LARGE}).
	 * {@code include} is a comma-separated list of {@code xml} and {@code pdf}; {@code null} means
	 * both. {@code resumo.csv} is the last file of the ZIP: a ZIP without it is incomplete.
	 */
	public CompletableFuture<Result<DownloadedFile>> download(String companyId, String month, String include) {
		return http.requestRaw(HttpMethod.GET, base(companyId), Query.of("month", month, "include", include));
	}

	private static String base(String companyId) {
		return "/companies/" + CompaniesResource.encode(companyId) + "/invoice-package";
	}
}
