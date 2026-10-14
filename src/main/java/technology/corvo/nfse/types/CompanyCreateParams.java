package technology.corvo.nfse.types;

/** Input for {@code companies.create}. */
public record CompanyCreateParams(
		String legalName,
		String taxId,
		String taxRegime,
		String tradeName,
		String municipalReg,
		String municipalityCode,
		Boolean simpleNational,
		String specialRegime,
		CompanyAddressInput address) {

	public static CompanyCreateParams of(String legalName, String taxId, String taxRegime) {
		return new CompanyCreateParams(legalName, taxId, taxRegime, null, null, null, null, null, null);
	}
}
