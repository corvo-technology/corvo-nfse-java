package technology.corvo.nfse.types;

public record CreditBalance(
	Integer avulsoBalance,
	Integer balance,
	Integer hardLimit,
	Integer planRemaining) {}
