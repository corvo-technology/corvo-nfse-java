package technology.corvo.nfse.types;

import java.util.Map;

public record IntegrationRule(
	Boolean allowAnonymousCustomer,
	Boolean cancelOnRefund,
	String externalProductId,
	String id,
	Boolean sendEmail,
	Map<String, Object> service) {}
