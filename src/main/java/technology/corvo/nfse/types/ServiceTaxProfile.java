package technology.corvo.nfse.types;

import java.util.List;

public record ServiceTaxProfile(
	String catalogVersion,
	String classCode,
	String classificationSource,
	String companyId,
	String createdAt,
	String cst,
	String effectiveFrom,
	String effectiveUntil,
	String id,
	String incidenceMunicipalityCode,
	String issRate,
	String issTaxation,
	Boolean issWithholdingDefault,
	String municipalServiceCode,
	String name,
	String nbsCode,
	String operationIndicator,
	String operationNature,
	List<String> regimeApplicability,
	String reviewStatus,
	String serviceCode,
	String tenantId,
	String withholdingPolicy,
	WithholdingRates withholdingRates) {}
