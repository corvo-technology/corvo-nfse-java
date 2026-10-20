package technology.corvo.nfse.types;

/**
 * Input for {@code invoices.cancel}.
 *
 * @param reason required: why the invoice is being cancelled
 * @param description optional for a {@code queued} invoice; required (15 to 255 characters) by the
 *     API to cancel an {@code authorized} or {@code processing} invoice. Not validated client-side.
 */
public record CancelInvoiceParams(CancellationReason reason, String description) {

	/** Cancels with a reason and no description (enough for a {@code queued} invoice). */
	public static CancelInvoiceParams of(CancellationReason reason) {
		return new CancelInvoiceParams(reason, null);
	}

	public CancelInvoiceParams withDescription(String description) {
		return new CancelInvoiceParams(reason, description);
	}
}
