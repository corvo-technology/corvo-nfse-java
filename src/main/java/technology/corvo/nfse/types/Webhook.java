package technology.corvo.nfse.types;

import java.util.List;

/** A webhook configuration. */
public record Webhook(String id, String url, List<String> events, Boolean active, String createdAt, String updatedAt) {

	/** The full set of events the Corvo API can deliver. */
	public static final List<String> WEBHOOK_EVENTS =
			List.of(
					"nfse.invoice.authorized",
					"nfse.invoice.rejected",
					"nfse.invoice.failed",
					"nfse.invoice.reconciliation_required",
					"nfse.invoice.manual_review",
					"nfse.invoice.documents_ready",
					"nfse.invoice.email_sent",
					"nfse.invoice.email_failed",
					"webhook.test");
}
