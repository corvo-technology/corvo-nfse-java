package technology.corvo.nfse.types;

import java.util.Map;

public record Invoice(
	String accessKey,
	Double amount,
	String authorizedAt,
	String cancellationDescription,
	String cancellationReason,
	String cancellationRequestedAt,
	String cancelledAt,
	String companyId,
	String competenceDate,
	String createdAt,
	String currency,
	CustomerSnapshot customerSnapshot,
	Double deduction,
	Double discount,
	String documentsReadyAt,
	String documentsStatus,
	String dpsId,
	Double dpsNumber,
	String dpsSeries,
	EmailSnapshot emailSnapshot,
	String emailStatus,
	String environment,
	String externalId,
	String fiscalProvider,
	String fiscalSchemaVersion,
	String id,
	String idempotencyKey,
	String issuedAt,
	String lastEmailSentAt,
	String lastEmailStatus,
	String lastErrorCode,
	String lastErrorMessage,
	String lastErrorProvider,
	Double nfseNumber,
	String portalFlag,
	String scheduledFor,
	ServiceSnapshot serviceSnapshot,
	String stage,
	String status,
	String substitutesInvoiceId,
	String substitutionDescription,
	Map<String, Object> taxSnapshot,
	String tenantId,
	String updatedAt) {

	/**
	 * Whether the e-mail to the customer was requested.
	 */
	public record EmailSnapshot(
		Boolean send) {}
}
