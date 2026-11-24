package technology.corvo.nfse.types;

import java.util.List;

public record InvoiceOverview(
	OverviewCounts counts,
	List<OverviewDay> dailyActivity,
	String from,
	String period,
	List<Invoice> recentInvoices,
	String timezone,
	String to) {}
