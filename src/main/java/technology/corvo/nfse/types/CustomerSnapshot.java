package technology.corvo.nfse.types;

import java.util.Map;

public record CustomerSnapshot(
	Address address,
	String email,
	Map<String, String> foreign,
	String name,
	String taxId) {

	public record Address(
		String city,
		String country,
		String state) {}
}
