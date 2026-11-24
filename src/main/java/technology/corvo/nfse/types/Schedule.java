package technology.corvo.nfse.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record Schedule(
	@JsonProperty("anchor_on") String anchorOn,
	Double attempts,
	@JsonProperty("company_id") String companyId,
	@JsonProperty("created_at") String createdAt,
	@JsonProperty("ends_on") String endsOn,
	String frequency,
	String id,
	Map<String, Object> invoice,
	String kind,
	@JsonProperty("last_run_on") String lastRunOn,
	@JsonProperty("next_run_on") String nextRunOn,
	@JsonProperty("pause_reason") String pauseReason,
	String status) {}
