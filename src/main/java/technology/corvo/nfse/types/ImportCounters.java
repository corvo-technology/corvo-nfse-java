package technology.corvo.nfse.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ImportCounters(
	Integer accepted,
	Integer authorized,
	@JsonProperty("authorized_amount") Double authorizedAmount,
	@JsonProperty("awaiting_amount") Double awaitingAmount,
	@JsonProperty("awaiting_credits") Integer awaitingCredits,
	Integer failed,
	@JsonProperty("failed_amount") Double failedAmount,
	Integer invalid,
	Integer pending,
	@JsonProperty("pending_amount") Double pendingAmount,
	Integer total,
	Integer valid,
	@JsonProperty("valid_amount") Double validAmount) {}
