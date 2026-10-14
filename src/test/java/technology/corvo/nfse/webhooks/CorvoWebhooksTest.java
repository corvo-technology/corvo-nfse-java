package technology.corvo.nfse.webhooks;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CorvoWebhooksTest {

	// Shared known vector across all Corvo SDKs.
	private static final String BODY = "{\"type\":\"nfse.invoice.authorized\",\"data\":{\"id\":\"inv_1\"}}";
	private static final String SECRET = "whsec_test_123";
	private static final String EXPECTED =
			"sha256=8f9bd040d8f717b19618ab3395ddfa4b8c176b8dc863195227b88dd85426bbbc";

	@Test
	void matchesKnownVector() {
		assertThat(CorvoWebhooks.verifySignature(BODY, EXPECTED, SECRET)).isTrue();
	}

	@Test
	void matchesKnownVectorWithBytesBody() {
		assertThat(
				CorvoWebhooks.verifySignature(
						BODY.getBytes(StandardCharsets.UTF_8), EXPECTED, SECRET))
				.isTrue();
	}

	@Test
	void toleratesWhitespaceAroundHeader() {
		assertThat(CorvoWebhooks.verifySignature(BODY, "  " + EXPECTED + "  ", SECRET)).isTrue();
	}

	@Test
	void rejectsTamperedBody() {
		String tampered = BODY.replace("inv_1", "inv_2");
		assertThat(CorvoWebhooks.verifySignature(tampered, EXPECTED, SECRET)).isFalse();
	}

	@Test
	void rejectsWrongSecret() {
		assertThat(CorvoWebhooks.verifySignature(BODY, EXPECTED, "whsec_other")).isFalse();
	}

	@Test
	void rejectsMalformedHeader() {
		assertThat(CorvoWebhooks.verifySignature(BODY, "sha256=xyz", SECRET)).isFalse();
		assertThat(CorvoWebhooks.verifySignature(BODY, "md5=abc", SECRET)).isFalse();
		assertThat(CorvoWebhooks.verifySignature(BODY, "", SECRET)).isFalse();
		assertThat(CorvoWebhooks.verifySignature(BODY, null, SECRET)).isFalse();
		// hex of the right length but wrong digest
		assertThat(
				CorvoWebhooks.verifySignature(
						BODY, "sha256=" + "0".repeat(64), SECRET))
				.isFalse();
	}

	@Test
	void rejectsNullOrEmptySecret() {
		assertThat(CorvoWebhooks.verifySignature(BODY, EXPECTED, null)).isFalse();
		assertThat(CorvoWebhooks.verifySignature(BODY, EXPECTED, "")).isFalse();
	}

	@Test
	void rejectsNullBody() {
		assertThat(CorvoWebhooks.verifySignature((byte[]) null, EXPECTED, SECRET)).isFalse();
		assertThat(CorvoWebhooks.verifySignature((String) null, EXPECTED, SECRET)).isFalse();
	}

	@Test
	void signProducesKnownVector() {
		assertThat(CorvoWebhooks.sign(BODY.getBytes(StandardCharsets.UTF_8), SECRET)).isEqualTo(EXPECTED);
	}
}
