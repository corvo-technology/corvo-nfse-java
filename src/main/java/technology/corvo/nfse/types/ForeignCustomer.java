package technology.corvo.nfse.types;

public record ForeignCustomer(
	String city,
	String complement,
	String country,
	String district,
	String noTaxIdReason,
	String number,
	String postalCode,
	String region,
	String street,
	String taxId) {}
