package technology.corvo.nfse.types;

import java.util.List;

public record Company(
	String addressCity,
	String addressComplement,
	String addressDistrict,
	String addressNumber,
	String addressState,
	String addressStreet,
	String addressZip,
	String createdAt,
	String defaultServiceCode,
	String environment,
	String fiscalSchemaVersion,
	String id,
	String legalName,
	MunicipalityAgreement municipalityAgreement,
	String municipalityCode,
	String municipalReg,
	Boolean sendMunicipalReg,
	List<String> setupGaps,
	Boolean simpleNational,
	String simplesIssRate,
	String simplesRate,
	String specialRegime,
	String status,
	String taxId,
	String taxRegime,
	String tenantId,
	String tradeName,
	String updatedAt,
	List<RegistryWarning> warnings) {}
