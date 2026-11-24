package technology.corvo.nfse.resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import technology.corvo.nfse.transport.DownloadedFile;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.ImportJob;
import technology.corvo.nfse.types.ImportRow;
import technology.corvo.nfse.types.ImportStart;
import technology.corvo.nfse.types.Paginated;
import technology.corvo.nfse.types.UpdateImportRow;

/** Spreadsheet imports: upload a CSV or Excel file, review the rows, emit. Fields are {@code snake_case}. */
public final class ImportsResource {

	private static final String BASE = "/imports";

	private final HttpTransport http;

	public ImportsResource(HttpTransport http) {
		this.http = http;
	}

	/**
	 * Starts an import from a {@code .csv} or {@code .xlsx} file (up to 2,000 rows and 10 MB), sent as
	 * {@code multipart/form-data}. {@code companyId} is the company that issues every row without a
	 * CNPJ column value; it may be {@code null}.
	 *
	 * @param fileName file name, with its extension (decides CSV or Excel); defaults to {@code import.csv}
	 */
	public CompletableFuture<Result<ImportStart>> start(byte[] file, String fileName, String companyId) {
		String name = fileName == null || fileName.isEmpty() ? "import.csv" : fileName;
		String boundary = "corvo-" + UUID.randomUUID();
		HttpTransport.RawBody raw =
				new HttpTransport.RawBody(
						buildMultipart(boundary, file, name, companyId), "multipart/form-data; boundary=" + boundary);
		return http.request(HttpMethod.POST, BASE, raw, null, null, UnwrapMode.DATA, ImportStart.class);
	}

	/** Lists the recent imports with their counters (newest first, at most 20). */
	public CompletableFuture<Result<List<ImportJob>>> list(Integer limit) {
		return http.requestList(HttpMethod.GET, BASE, null, Query.of("limit", limit), ImportJob.class);
	}

	/** Status and counters of an import. */
	public CompletableFuture<Result<ImportJob>> get(String id) {
		return http.request(HttpMethod.GET, path(id), null, null, null, UnwrapMode.DATA, ImportJob.class);
	}

	/** Emits every valid row of the import. {@code sendEmail} emails each issued invoice (may be {@code null}). */
	public CompletableFuture<Result<ImportJob>> emit(String id, Boolean sendEmail) {
		Map<String, Object> body = new LinkedHashMap<>();
		if (sendEmail != null) {
			body.put("sendEmail", sendEmail);
		}
		return http.request(HttpMethod.POST, path(id) + "/emit", body, null, null, UnwrapMode.DATA,
				ImportJob.class);
	}

	/**
	 * Lists one page of the rows of an import (default 50, at most 200). {@code status} is {@code
	 * valid}, {@code invalid}, {@code queued}, {@code accepted} or {@code rejected}.
	 */
	public CompletableFuture<Result<Paginated<ImportRow>>> listRows(
			String id, String status, Integer limit, String cursor) {
		return http.list(path(id) + "/rows", Query.of("status", status, "limit", limit, "cursor", cursor),
				UnwrapMode.NONE, ImportRow.class);
	}

	/**
	 * Walks every row of an import lazily.
	 *
	 * <p><b>Documented exception to the never-throw rule:</b> a page failure throws {@code CorvoApiError}.
	 */
	public Stream<ImportRow> iterateRows(String id, String status, Integer limit, String cursor) {
		return http.iterate(path(id) + "/rows", Query.of("status", status, "limit", limit, "cursor", cursor),
				UnwrapMode.NONE, ImportRow.class);
	}

	/** Corrects one row ({@code line}) and checks it again (only while the import is ready). */
	public CompletableFuture<Result<ImportRow>> updateRow(String id, int line, UpdateImportRow params) {
		return http.request(HttpMethod.PATCH, path(id) + "/rows/" + line, params, null, null, UnwrapMode.DATA,
				ImportRow.class);
	}

	/** Discards an import that has not been emitted. */
	public CompletableFuture<Result<Void>> discard(String id) {
		return http.request(HttpMethod.DELETE, path(id), null, null, null, UnwrapMode.DATA, Void.class);
	}

	/** Downloads the spreadsheet template (CSV). */
	public CompletableFuture<Result<DownloadedFile>> downloadTemplate() {
		return http.requestRaw(HttpMethod.GET, BASE + "/template", null);
	}

	/** Downloads the CSV with the rows that need attention. */
	public CompletableFuture<Result<DownloadedFile>> downloadErrors(String id) {
		return http.requestRaw(HttpMethod.GET, path(id) + "/errors.csv", null);
	}

	private static String path(String id) {
		return BASE + "/" + CompaniesResource.encode(id);
	}

	/** {@code file} (the bytes) followed by the optional {@code companyId} text field. */
	static byte[] buildMultipart(String boundary, byte[] file, String fileName, String companyId) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			if (companyId != null && !companyId.isEmpty()) {
				write(out, "--" + boundary + "\r\n");
				write(out, "Content-Disposition: form-data; name=\"companyId\"\r\n\r\n");
				write(out, companyId);
				write(out, "\r\n");
			}
			write(out, "--" + boundary + "\r\n");
			write(out, "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n");
			write(out, "Content-Type: " + contentTypeOf(fileName) + "\r\n\r\n");
			out.write(file);
			write(out, "\r\n--" + boundary + "--\r\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return out.toByteArray();
	}

	private static String contentTypeOf(String fileName) {
		return fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")
				? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
				: "text/csv";
	}

	private static void write(ByteArrayOutputStream out, String value) throws IOException {
		out.write(value.getBytes(StandardCharsets.UTF_8));
	}
}
