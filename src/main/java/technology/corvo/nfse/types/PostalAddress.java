package technology.corvo.nfse.types;

public record PostalAddress(
	String city,
	String complement,
	String district,
	String number,
	String state,
	String street,
	String zip) {}
