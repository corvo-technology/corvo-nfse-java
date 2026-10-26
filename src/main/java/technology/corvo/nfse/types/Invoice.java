package technology.corvo.nfse.types;

import java.util.Map;

/**
 * An NFS-e invoice. {@code scheduledFor} is when a scheduled invoice is released (ISO), or {@code
 * null} for an immediate one. {@code discount} and {@code deduction} are set only when declared.
 * The {@code cancellation*} fields are {@code null} until a cancellation is requested; {@code
 * cancelledAt} is when the invoice reached {@code cancelled}.
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
		Double discount,
		Double deduction,
		String cancellationReason,
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
