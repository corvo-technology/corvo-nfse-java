package technology.corvo.nfse.types;

import java.util.List;

/** A webhook configuration. */
public record Webhook(String id, String url, List<String> events, Boolean active, String createdAt, String updatedAt) {

	/**
	 * The events a webhook can subscribe to. {@code webhook.test} is only sent by {@code
	 * webhooks().test()} and cannot be subscribed.
	 */
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
					"nfse.invoice.fiscal_discrepancy");
}
