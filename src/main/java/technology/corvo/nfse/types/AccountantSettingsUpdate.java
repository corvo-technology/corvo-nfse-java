package technology.corvo.nfse.types;

public record AccountantSettingsUpdate(
	String email,
	String lastSentMonth,
	Boolean testEmailSent) {}
