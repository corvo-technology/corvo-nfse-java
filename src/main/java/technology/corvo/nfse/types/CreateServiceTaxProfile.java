package technology.corvo.nfse.types;

import java.util.List;
import java.util.Map;

public record CreateServiceTaxProfile(
	String catalogVersion,
	String classCode,
	String classificationSource,
	String cst,
	String effectiveFrom,
	String effectiveUntil,
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
	String serviceCode,
	String withholdingPolicy,
	Map<String, String> withholdingRates) {}
