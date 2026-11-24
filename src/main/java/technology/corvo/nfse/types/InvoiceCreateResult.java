package technology.corvo.nfse.types;

/**
 * What {@code invoices.create} answers: an {@link Invoice} for an immediate emission, or a {@link
 * Schedule} when {@code options.type} is {@code SCHEDULE} or {@code RECURRING} (the note only exists
 * when the schedule runs; follow the runs with {@code schedules().listRuns}). Exactly one of the two
 * is set.
 */
public record InvoiceCreateResult(Invoice invoice, Schedule schedule) {

	/** {@code true} when a schedule was created instead of an invoice. */
	public boolean isSchedule() {
		return schedule != null;
	}
}
