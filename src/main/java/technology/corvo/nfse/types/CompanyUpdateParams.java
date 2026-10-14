package technology.corvo.nfse.types;

/**
 * Input for {@code companies.update}. Only non-null fields are sent; a {@code null} string means
 * "don't change". To explicitly clear a field such as {@code municipalReg}, the API accepts
 * {@code ""} — send that value.
 */
public record CompanyUpdateParams(
		String legalName,
		String tradeName,
		String municipalReg,
		String municipalityCode,
		String taxRegime,
		Boolean simpleNational,
		String specialRegime,
		CompanyAddressInput address) {}
