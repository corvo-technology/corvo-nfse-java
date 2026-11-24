package technology.corvo.nfse.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ImportStart(
	@JsonProperty("duplicate_of") String duplicateOf,
	String id,
	String status) {}
