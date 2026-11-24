package technology.corvo.nfse.types;

import java.util.List;

public record ReviewInvoices(
	String action,
	List<String> invoiceIds) {}
