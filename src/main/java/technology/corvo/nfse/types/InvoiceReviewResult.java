package technology.corvo.nfse.types;

import java.util.List;

public record InvoiceReviewResult(
	List<ResultsItem> results) {

	public record ResultsItem(
		String id,
		String outcome,
		String reason) {}
}
