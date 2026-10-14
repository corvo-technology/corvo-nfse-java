package technology.corvo.nfse.resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.Certificate;
import technology.corvo.nfse.types.Paginated;

/** Fiscal certificate (A1/PKCS#12) operations, scoped to a company. */
public final class CertificatesResource {

	private static final String BASE = "/companies";

	private final HttpTransport http;

	public CertificatesResource(HttpTransport http) {
		this.http = http;
	}

	/**
	 * Uploads a PKCS#12 certificate via {@code multipart/form-data}. Fields: {@code file} (the
	 * .p12 bytes, max 50 KiB) and {@code password}. The caller never touches multipart.
	 *
	 * @param file raw .p12/.pfx bytes
	 * @param password certificate password
	 * @param fileName optional filename; defaults to {@code certificate.p12}
	 */
	public CompletableFuture<Result<Certificate>> upload(
			String companyId, byte[] file, String password, String fileName) {
		String effectiveFileName = fileName == null || fileName.isEmpty() ? "certificate.p12" : fileName;
		String boundary = "corvo-" + UUID.randomUUID();
		byte[] body = buildMultipart(boundary, file, effectiveFileName, password);
		HttpTransport.RawBody raw =
				new HttpTransport.RawBody(body, "multipart/form-data; boundary=" + boundary);
		return http.request(
				HttpMethod.POST,
				BASE + "/" + CompaniesResource.encode(companyId) + "/certificates",
				raw,
				null,
				null,
				UnwrapMode.DATA,
				Certificate.class);
	}

	/** Lists one page of certificates for a company. */
	public CompletableFuture<Result<Paginated<Certificate>>> list(
			String companyId, Integer limit, String cursor) {
		return http.list(
				BASE + "/" + CompaniesResource.encode(companyId) + "/certificates",
				CompaniesResource.listQuery(limit, cursor),
				UnwrapMode.DATA,
				Certificate.class);
	}

	/** Deletes a certificate. 204 responses yield {@code Ok(null)}. */
	public CompletableFuture<Result<Void>> remove(String companyId, String certificateId) {
		return http.request(
				HttpMethod.DELETE,
				BASE + "/" + CompaniesResource.encode(companyId) + "/certificates/"
						+ CompaniesResource.encode(certificateId),
				null,
				null,
				null,
				UnwrapMode.DATA,
				Void.class);
	}

	/**
	 * Builds a {@code multipart/form-data} body by hand: {@code file} (bytes, application/x-pkcs12)
	 * followed by {@code password} (plain text).
	 */
	static byte[] buildMultipart(String boundary, byte[] file, String fileName, String password) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			write(out, "--" + boundary + "\r\n");
			write(out, "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n");
			write(out, "Content-Type: application/x-pkcs12\r\n\r\n");
			out.write(file);
			write(out, "\r\n--" + boundary + "\r\n");
			write(out, "Content-Disposition: form-data; name=\"password\"\r\n\r\n");
			write(out, password);
			write(out, "\r\n--" + boundary + "--\r\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return out.toByteArray();
	}

	private static void write(ByteArrayOutputStream out, String value) throws IOException {
		out.write(value.getBytes(StandardCharsets.UTF_8));
	}
}
