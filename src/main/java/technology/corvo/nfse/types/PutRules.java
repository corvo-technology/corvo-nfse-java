package technology.corvo.nfse.types;

import java.util.List;

public record PutRules(
	List<IntegrationRuleInput> rules) {}
