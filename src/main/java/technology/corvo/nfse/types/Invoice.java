package technology.corvo.nfse.types;

import java.util.Map;

/**
 * An NFS-e invoice. {@code scheduledFor} is when a scheduled invoice is released (ISO), or {@code
 * null} for an immediate one.
 *
 * <p>{@code status} is one of {@code queued}, {@code processing}, {@code authorized}, {@code
 * rejected}, {@code retry_pending}, {@code reconciliation_required}, {@code manual_review}, {@code
 * failed}, {@code cancelling} or {@code cancelled}. It is a plain string, so a status added by a
 * newer API never breaks deserialization.
 *
 * <p>Cancellation fields are {@code null} until {@code invoices().cancel(...)} is requested:
 * {@code cancellationReason} and {@code cancellationDescription} echo the request, {@code
 * cancellationRequestedAt} is when it was registered (also set while a {@code processing} invoice
 * is still being issued), and {@code cancelledAt} is when the invoice reached {@code cancelled}.
 * If the Sefin rejects the cancellation, the invoice goes back to {@code authorized} with {@code
 * lastErrorCode}/{@code lastErrorMessage} set.
 */
public record Invoice(
		String id,
		String companyId,
		String tenantId,
		String environment,
		Integer documentKeyVersion,
		EmailSnapshot emailSnapshot,
		String externalId,
		String idempotencyKey,
		String status,
		CustomerSnapshot customerSnapshot,
		ServiceSnapshot serviceSnapshot,
		Map<String, Object> taxSnapshot,
		Double amount,
		String currency,
		String competenceDate,
		String dpsId,
		String dpsSeries,
		Integer dpsNumber,
		String accessKey,
		Integer nfseNumber,
		String fiscalProvider,
		String fiscalSchemaVersion,
		String issuedAt,
		String authorizedAt,
		String lastErrorCode,
		String lastErrorMessage,
		String lastErrorProvider,
		String documentsReadyAt,
		String lastEmailStatus,
		String lastEmailSentAt,
		String createdAt,
		String updatedAt,
		String documentsStatus,
		String emailStatus,
		String stage,
		String scheduledFor,
		CancellationReason cancellationReason,
		String cancellationDescription,
		String cancelledAt,
		String cancellationRequestedAt) {

	public record EmailSnapshot(Boolean send) {}

	public record CustomerSnapshot(
			String name, String email, String taxId, CustomerAddress address) {}

	public record CustomerAddress(
			String street, String city, String state, String zip, String country) {}

	public record ServiceSnapshot(String code, String description, String municipalityCode) {}
}
