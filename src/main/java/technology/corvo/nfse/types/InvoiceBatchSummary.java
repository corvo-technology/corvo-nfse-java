package technology.corvo.nfse.types;

import java.util.List;

/**
 * Result of {@code invoices.createBatch}. {@code results} has one entry per item, in the order
 * sent.
 */
public record InvoiceBatchSummary(
		Integer total, Integer accepted, Integer rejected, List<Result> results) {

	/**
	 * Outcome of one item.
	 *
	 * @param index position of the item in the request, starting at 0
	 * @param status {@code "accepted"} (then {@code invoice} is set) or {@code "rejected"} (then
	 *     {@code error} is set and the item was not saved)
	 * @param replayed {@code true} when the item repeated an idempotency key already saved with the
	 *     same content
	 */
	public record Result(Integer index, String status, Boolean replayed, Invoice invoice, Error error) {}

	/** Why an item was rejected. {@code details} holds the field-level issues, when there are any. */
	public record Error(String code, String message, Object details) {}
}
