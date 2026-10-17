package technology.corvo.nfse.types;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * The pre-emission estimate of {@code invoices.calculate}. {@code status} is {@code "ready"} or
 * {@code "blocked"}: a fiscal gap is data, not an error. Money values are decimal strings.
 */
public record InvoiceCalculation(
		Boolean preEmission,
		String status,
		String disclaimer,
		String competenceDate,
		String taxRegime,
		JsonNode classification,
		JsonNode applicability,
		Taxes taxes,
		List<JsonNode> withholdings,
		String netReceivableAmount,
		Boolean collectionWaived,
		JsonNode calculation,
		List<JsonNode> warnings,
		List<PendingField> pendingFields,
		Blocking blocking,
		List<JsonNode> trace) {

	/** One tax. {@code rate} is {@code null} on {@code ibsTotal}, which sums the state and municipal IBS. */
	public record TaxAmount(String base, String rate, String amount) {}

	public record Taxes(
			TaxAmount iss, TaxAmount cbs, TaxAmount ibsState, TaxAmount ibsCity, TaxAmount ibsTotal) {}

	public record Blocking(String code, String message, String step) {}

	public record PendingField(String field, String reason) {}
}
