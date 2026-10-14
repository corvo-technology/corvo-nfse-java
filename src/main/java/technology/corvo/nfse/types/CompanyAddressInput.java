package technology.corvo.nfse.types;

/** Company address, as accepted on create/update input. */
public record CompanyAddressInput(
		String street, String number, String complement, String district, String city, String state, String zip) {

	public static CompanyAddressInput of(String street, String district, String city, String state, String zip) {
		return new CompanyAddressInput(street, null, null, district, city, state, zip);
	}
}
