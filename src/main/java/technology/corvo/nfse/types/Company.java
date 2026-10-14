package technology.corvo.nfse.types;

import java.util.List;

/**
 * A company (emitter) registered with the Corvo NFS-e API.
 *
 * @param setupGaps list of setup steps still missing for this company
 */
public record Company(
		String id,
		String tenantId,
		String legalName,
		String tradeName,
		String taxId,
		String municipalReg,
		String municipalityCode,
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
