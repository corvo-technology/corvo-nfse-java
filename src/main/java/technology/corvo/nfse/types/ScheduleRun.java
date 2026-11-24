package technology.corvo.nfse.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScheduleRun(
	Double attempt,
	@JsonProperty("created_at") String createdAt,
	@JsonProperty("error_code") String errorCode,
	@JsonProperty("error_message") String errorMessage,
	String id,
	@JsonProperty("invoice_id") String invoiceId,
	@JsonProperty("occurrence_on") String occurrenceOn,
	String outcome,
	@JsonProperty("ran_on") String ranOn) {}
