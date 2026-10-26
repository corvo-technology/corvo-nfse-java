package technology.corvo.nfse.types;

/**
 * Input for {@code invoices.cancel}.
 *
 * @param reason {@code "issue_error"}, {@code "service_not_provided"} or {@code "other"}
 * @param description 15 to 255 characters; required to cancel an authorized or processing invoice,
 *     optional for a queued one
 */
public record InvoiceCancelParams(String reason, String description) {

	public static InvoiceCancelParams of(String reason) {
		return new InvoiceCancelParams(reason, null);
	}

	public static InvoiceCancelParams of(String reason, String description) {
		return new InvoiceCancelParams(reason, description);
	}
}
