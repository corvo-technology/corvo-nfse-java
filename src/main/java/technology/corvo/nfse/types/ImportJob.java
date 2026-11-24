package technology.corvo.nfse.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ImportJob(
	@JsonProperty("company_id") String companyId,
	ImportCounters counters,
	@JsonProperty("created_at") String createdAt,
	@JsonProperty("emit_finished_at") String emitFinishedAt,
	@JsonProperty("emit_started_at") String emitStartedAt,
	@JsonProperty("error_code") String errorCode,
	@JsonProperty("file_name") String fileName,
	String id,
	@JsonProperty("rows_expected") Integer rowsExpected,
	String status,
	@JsonProperty("unknown_columns") List<String> unknownColumns) {}
