package technology.corvo.nfse.resources;

import java.util.LinkedHashMap;
import java.util.Map;

/** Builds a query map from alternating key/value arguments; {@code null} values are dropped later. */
final class Query {

	private Query() {}

	static Map<String, Object> of(Object... keyValues) {
		Map<String, Object> query = new LinkedHashMap<>();
		for (int i = 0; i + 1 < keyValues.length; i += 2) {
			query.put((String) keyValues[i], keyValues[i + 1]);
		}
		return query;
	}
}
