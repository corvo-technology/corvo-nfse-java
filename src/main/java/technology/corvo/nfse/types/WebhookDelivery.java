package technology.corvo.nfse.types;

import com.fasterxml.jackson.databind.JsonNode;

/** A single webhook delivery attempt record. */
public record WebhookDelivery(
		String id,
		String tenantId,
		String webhookConfigId,
		String eventType,
		String environment,
		String url,
		JsonNode payload,
		Integer statusCode,
		JsonNode responseBody,
		String errorMessage,
		String status,
		Integer durationMs,
		Integer retryCount,
		String nextRetryAt,
		String lastError,
		String createdAt) {}
