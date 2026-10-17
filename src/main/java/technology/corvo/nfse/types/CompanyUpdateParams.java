package technology.corvo.nfse.types;

/**
 * Input for {@code companies.update}. Only non-null fields are sent; a {@code null} field means
 * "don't change" (clearing a field is not supported by this record).
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
		CompanyAddressInput address) {}
