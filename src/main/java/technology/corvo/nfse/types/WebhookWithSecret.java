package technology.corvo.nfse.types;

import java.util.List;

/**
 * A webhook as returned by {@code webhooks.create} — includes the signing {@code secret}, which
 * is returned exactly once (at creation).
 */
public record WebhookWithSecret(
		String id,
		String url,
		List<String> events,
		Boolean active,
		String createdAt,
		String updatedAt,
		String secret) {}
