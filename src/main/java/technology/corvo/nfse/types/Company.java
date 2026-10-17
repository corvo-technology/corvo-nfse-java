package technology.corvo.nfse.types;

import java.util.List;

/**
 * A company (emitter) registered with the Corvo NFS-e API.
 *
 * <p>Optional fields are absent from the JSON when unset and read as {@code null}.
 *
 * @param setupGaps what is still pending for this company: {@code municipality_code}, {@code
 *     certificate}
 */
public record Company(
		String id,
		String tenantId,
		String legalName,
		String tradeName,
		String taxId,
		String municipalReg,
		Boolean sendMunicipalReg,
		String municipalityCode,
		String defaultServiceCode,
		String taxRegime,
		Boolean simpleNational,
		String specialRegime,
		String addressStreet,
		String addressNumber,
		String addressComplement,
		String addressDistrict,
		String addressCity,
		String addressState,
		String addressZip,
		String fiscalSchemaVersion,
		String environment,
		String status,
		String createdAt,
		String updatedAt,
		String deletedAt,
		List<String> setupGaps) {}
