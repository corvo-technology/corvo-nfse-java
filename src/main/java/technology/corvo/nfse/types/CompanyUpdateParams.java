package technology.corvo.nfse.types;

/**
 * Input for {@code companies.update}. Only non-null fields are sent; a {@code null} field means
 * "don't change" (clearing a field is not supported by this record). {@code simplesRate} updates
 * the Simples rate (%), for example every month; it is ignored for {@code MEI}.
 */
public record CompanyUpdateParams(
		String legalName,
		String tradeName,
		String municipalReg,
		Boolean sendMunicipalReg,
		String municipalityCode,
		String defaultServiceCode,
		String taxRegime,
		Boolean simpleNational,
		String specialRegime,
		String simplesRate,
		String simplesIssRate,
		CompanyAddressInput address) {

	/** Without the Simples rates; kept so existing callers of the previous constructor still compile. */
	public CompanyUpdateParams(
			String legalName,
			String tradeName,
			String municipalReg,
			Boolean sendMunicipalReg,
			String municipalityCode,
			String defaultServiceCode,
			String taxRegime,
			Boolean simpleNational,
			String specialRegime,
			CompanyAddressInput address) {
		this(
				legalName, tradeName, municipalReg, sendMunicipalReg, municipalityCode, defaultServiceCode,
				taxRegime, simpleNational, specialRegime, null, null, address);
	}
}
