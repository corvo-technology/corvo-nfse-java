package technology.corvo.nfse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CorvoTest {

	@Test
	void buildsWithExplicitApiKey() {
		Corvo corvo = Corvo.builder().apiKey(TestHarness.API_KEY).baseUrl("https://api.corvo.tech").build();
		assertThat(corvo.companies()).isNotNull();
		assertThat(corvo.certificates()).isNotNull();
		assertThat(corvo.invoices()).isNotNull();
		assertThat(corvo.webhooks()).isNotNull();
		assertThat(corvo.credits()).isNotNull();
		assertThat(corvo.billing()).isNotNull();
		assertThat(corvo.health()).isNotNull();
	}

	@Test
	void throwsWithoutApiKey() {
		// Skips itself if CORVO_API_KEY is set in the environment (contract runs).
		if (System.getenv("CORVO_API_KEY") != null) {
			return;
		}
		assertThatThrownBy(() -> Corvo.builder().build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("apiKey is required");
	}

	@Test
	void rejectsHttpOnPublicHosts() {
		assertThatThrownBy(
						() ->
								Corvo.builder()
										.apiKey(TestHarness.API_KEY)
										.baseUrl("http://api.example.com")
										.build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("http:// is only allowed");
	}

	@Test
	void allowsHttpOnLoopbackAndPrivateHosts() {
		assertThatCode(
						() ->
								Corvo.builder()
										.apiKey(TestHarness.API_KEY)
										.baseUrl("http://localhost:4000")
										.build())
				.doesNotThrowAnyException();
		assertThatCode(
						() ->
								Corvo.builder()
										.apiKey(TestHarness.API_KEY)
										.baseUrl("http://192.168.1.10")
										.build())
				.doesNotThrowAnyException();
		assertThatCode(
						() ->
								Corvo.builder()
										.apiKey(TestHarness.API_KEY)
										.baseUrl("http://10.0.0.1")
										.build())
				.doesNotThrowAnyException();
	}

	@Test
	void allowsHttpOnPublicHostsWithOptOut() {
		assertThatCode(
						() ->
								Corvo.builder()
										.apiKey(TestHarness.API_KEY)
										.baseUrl("http://api.example.com")
										.allowInsecureHttp(true)
										.build())
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsMalformedBaseUrl() {
		assertThatThrownBy(
						() ->
								Corvo.builder()
										.apiKey(TestHarness.API_KEY)
										.baseUrl("not a url")
										.build())
				.isInstanceOf(IllegalArgumentException.class);
	}
}
