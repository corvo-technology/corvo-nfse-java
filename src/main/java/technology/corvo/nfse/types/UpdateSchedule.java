package technology.corvo.nfse.types;

import java.util.Map;

public record UpdateSchedule(
	String anchorOn,
	String endsOn,
	String frequency,
	Map<String, Object> invoice) {}
