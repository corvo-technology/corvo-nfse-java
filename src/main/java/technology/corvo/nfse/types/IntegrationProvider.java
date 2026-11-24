package technology.corvo.nfse.types;

import java.util.List;

public record IntegrationProvider(
	List<CredentialFieldsItem> credentialFields,
	String id,
	String name,
	List<String> supportedFlows,
	String version,
	WebhookInstructions webhookInstructions) {

	public record CredentialFieldsItem(
		Boolean generate,
		Boolean inWebhookUrl,
		String key,
		String label,
		Boolean revealOnce,
		Boolean secret) {}

	public record WebhookInstructions(
		String en,
		String pt) {}
}
