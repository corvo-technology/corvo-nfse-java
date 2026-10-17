package technology.corvo.nfse.types;

/**
 * Input for {@code companies.create}.
 *
 * @param taxId the CNPJ
 * @param municipalityCode IBGE code of the company's city (7 digits)
 * @param municipalReg stored, but only sent on the invoice when {@code sendMunicipalReg} is true
 * @param sendMunicipalReg default false: turn it on only if the city registered the company in the
 *     national registry
 * @param defaultServiceCode national service code (6 digits) used when an invoice omits {@code
 *     service.code}
 */
public record CompanyCreateParams(
		String legalName,
		String taxId,
		String taxRegime,
		String municipalityCode,
		String tradeName,
		String municipalReg,
		Boolean sendMunicipalReg,
		String defaultServiceCode,
		Boolean simpleNational,
		String specialRegime,
		CompanyAddressInput address) {

	public static CompanyCreateParams of(
			String legalName, String taxId, String taxRegime, String municipalityCode) {
		return new CompanyCreateParams(
				legalName, taxId, taxRegime, municipalityCode, null, null, null, null, null, null, null);
	}

	public CompanyCreateParams withDefaultServiceCode(String defaultServiceCode) {
		return new CompanyCreateParams(
				legalName, taxId, taxRegime, municipalityCode, tradeName, municipalReg, sendMunicipalReg,
				defaultServiceCode, simpleNational, specialRegime, address);
	}
}
