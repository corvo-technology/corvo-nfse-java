package technology.corvo.nfse.webhooks;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Webhook signature verification.
 *
 * <p>Corvo signs webhook deliveries with HMAC-SHA256 over the <b>raw</b> request body, using the
 * secret returned once at webhook creation. The signature arrives in the
 * {@code X-Corbo-Signature: sha256=<hex>} header. Compare in constant time with
 * {@link MessageDigest#isEqual}.
 */
public final class CorvoWebhooks {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final String PREFIX = "sha256=";

	private CorvoWebhooks() {}

	/**
	 * Verifies a {@code X-Corbo-Signature} header against the raw body and the webhook secret.
	 *
	 * <p>Pass the raw bytes exactly as received — re-serializing the body will change the HMAC and
	 * verification will fail.
	 *
	 * @param rawBody the raw request body bytes
	 * @param signatureHeader the {@code X-Corbo-Signature} header value ({@code sha256=<hex>});
	 *     {@code null}/malformed values return {@code false}
	 * @param secret the webhook secret returned at creation
	 * @return {@code true} when the signature matches, compared in constant time
	 */
	public static boolean verifySignature(byte[] rawBody, String signatureHeader, String secret) {
		if (rawBody == null || secret == null || secret.isEmpty() || signatureHeader == null) {
			return false;
		}
		String header = signatureHeader.trim();
		if (!header.startsWith(PREFIX)) {
			return false;
		}
		String receivedHex = header.substring(PREFIX.length());
		if (receivedHex.length() != 64) {
			return false;
		}
		byte[] received;
		try {
			received = HexFormat.of().parseHex(receivedHex.toLowerCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return false; // non-hex characters
		}
		byte[] expected = hmacSha256(secret, rawBody);
		if (received.length != expected.length) {
			return false;
		}
		return MessageDigest.isEqual(received, expected);
	}

	/** Convenience overload accepting the body as a UTF-8 string. */
	public static boolean verifySignature(String rawBody, String signatureHeader, String secret) {
		if (rawBody == null) {
			return false;
		}
		return verifySignature(rawBody.getBytes(StandardCharsets.UTF_8), signatureHeader, secret);
	}

	/** Computes {@code sha256=<hex>} for the given body and secret. Exposed for testing. */
	static String sign(byte[] rawBody, String secret) {
		return PREFIX + HexFormat.of().formatHex(hmacSha256(secret, rawBody));
	}

	private static byte[] hmacSha256(String secret, byte[] body) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
			return mac.doFinal(body);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("HMAC-SHA256 unavailable", e);
		}
	}
}
