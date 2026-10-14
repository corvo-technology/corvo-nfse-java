package technology.corvo.nfse.types;

/** An A1 (PKCS#12) fiscal certificate stored for a company. */
public record Certificate(
		String id,
		String companyId,
		String serialNumber,
		String issuer,
		String subject,
		String fingerprint,
		String validFrom,
		String validUntil,
		String status,
		String createdAt) {}
