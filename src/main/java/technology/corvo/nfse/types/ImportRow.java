package technology.corvo.nfse.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record ImportRow(
	Map<String, Object> data,
	@JsonProperty("error_code") String errorCode,
	@JsonProperty("error_message") String errorMessage,
	@JsonProperty("invoice_id") String invoiceId,
	Integer line,
	String status) {}
