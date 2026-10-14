package technology.corvo.nfse.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import technology.corvo.nfse.Corvo;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.types.Company;
import technology.corvo.nfse.types.Paginated;

/**
 * Optional live-contract test, verified against the same contract as the Node SDK. Skips itself
 * unless {@code CORVO_API_KEY} and {@code CORVO_BASE_URL} are set (e.g. a local Corvo instance
 * seeded with a test key).
 */
class ContractTest {

	@Test
	@EnabledIfEnvironmentVariable(named = "CORVO_API_KEY", matches = ".+")
	@EnabledIfEnvironmentVariable(named = "CORVO_BASE_URL", matches = ".+")
	void liveContractSmokeTest() throws Exception {
		Corvo corvo = Corvo.builder().build();

		Result<com.fasterxml.jackson.databind.JsonNode> live =
				corvo.health().live().get(10, TimeUnit.SECONDS);
		assertThat(live.isOk()).as("health.live must succeed").isTrue();

		Result<Paginated<Company>> companies =
				corvo.companies().list(1, null).get(10, TimeUnit.SECONDS);
		if (companies.isErr()) {
			// An unauthorized key must fail with a structured error, not an exception.
			assertThat(((Result.Err<Paginated<Company>>) companies).error().status()).isGreaterThan(0);
		} else {
			assertThat(((Result.Ok<Paginated<Company>>) companies).data().pagination().limit()).isEqualTo(1);
		}
	}
}
