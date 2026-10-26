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
 * @param taxRegime {@code "MEI"} or {@code "Simples Nacional"}: the only regimes Corvo serves
 * @param simplesRate required for {@code Simples Nacional}, ignored for {@code MEI}: effective total
 *     rate (%) of your DAS, as in the PGDAS-D, e.g. {@code "6.54"}
 * @param simplesIssRate {@code Simples Nacional} only: effective ISS rate (%) of your bracket (1.80
 *     to 5.00), needed when a customer withholds ISS
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
		String simplesRate,
		String simplesIssRate,
		CompanyAddressInput address) {

	/** Without the Simples rates; kept so existing callers of the previous constructor still compile. */
	public CompanyCreateParams(
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
		this(
				legalName, taxId, taxRegime, municipalityCode, tradeName, municipalReg, sendMunicipalReg,
				defaultServiceCode, simpleNational, specialRegime, null, null, address);
	}

	public static CompanyCreateParams of(
			String legalName, String taxId, String taxRegime, String municipalityCode) {
		return new CompanyCreateParams(
				legalName, taxId, taxRegime, municipalityCode, null, null, null, null, null, null, null, null, null);
	}

	public CompanyCreateParams withDefaultServiceCode(String defaultServiceCode) {
		return new CompanyCreateParams(
				legalName, taxId, taxRegime, municipalityCode, tradeName, municipalReg, sendMunicipalReg,
				defaultServiceCode, simpleNational, specialRegime, simplesRate, simplesIssRate, address);
	}

	/** Required for {@code Simples Nacional}: the effective DAS rate (%), e.g. {@code "6.54"}. */
	public CompanyCreateParams withSimplesRate(String simplesRate) {
		return new CompanyCreateParams(
				legalName, taxId, taxRegime, municipalityCode, tradeName, municipalReg, sendMunicipalReg,
				defaultServiceCode, simpleNational, specialRegime, simplesRate, simplesIssRate, address);
	}

	public CompanyCreateParams withSimplesIssRate(String simplesIssRate) {
		return new CompanyCreateParams(
				legalName, taxId, taxRegime, municipalityCode, tradeName, municipalReg, sendMunicipalReg,
				defaultServiceCode, simpleNational, specialRegime, simplesRate, simplesIssRate, address);
	}
}
