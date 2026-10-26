package technology.corvo.nfse.types;

/**
 * One item of {@code invoices.createBatch}: the fields of {@link InvoiceCreateParams} plus its own
 * idempotency key.
 *
 * @param invoice the invoice to issue
 * @param idempotencyKey up to 255 characters; repeating it with the same content returns the saved
 *     invoice with {@code replayed = true} instead of issuing again. The {@code Idempotency-Key}
 *     header is not used by the batch route.
 */
public record InvoiceBatchItem(InvoiceCreateParams invoice, String idempotencyKey) {

	public static InvoiceBatchItem of(InvoiceCreateParams invoice, String idempotencyKey) {
		return new InvoiceBatchItem(invoice, idempotencyKey);
	}
}
