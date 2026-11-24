package technology.corvo.nfse.types;

/**
 * Percent strings, e.g. "1.5".
 */
public record WithholdingRates(
	String cofins,
	String csll,
	String inss,
	String irrf,
	String pis) {}
