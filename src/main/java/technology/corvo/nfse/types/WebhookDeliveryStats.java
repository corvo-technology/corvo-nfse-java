package technology.corvo.nfse.types;

/** Aggregated webhook delivery counters for a time window. */
public record WebhookDeliveryStats(int total, int sent, int failed, int pending, int delayed) {}
