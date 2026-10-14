package technology.corvo.nfse.error;

import java.time.Duration;
import java.util.List;

/**
 * Typed error for the Corvo NFS-e API.
 *
 * <p>Carries at minimum the HTTP {@code status} (0 for network/timeout), the stable string
 * {@code code} ({@code network_error} / {@code timeout} for transport failures), a human-readable
 * {@code message}, the {@code requestId}, field-level {@code details}, and {@code retryAfter}.
 *
 * <p>This is the SDK's only exception type. Normal API, network, and timeout failures never
 * surface as exceptions — they are returned inside {@code Result.Err}. The single documented
 * exception is the {@code iterate(...)} pagination helpers, which throw {@code CorvoApiError}
 * (unchecked) on page failure because a {@code Stream<T>} has no other channel to report it.
 */
public class CorvoApiError extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public static final String CODE_NETWORK = "network_error";
	public static final String CODE_TIMEOUT = "timeout";
	public static final String CODE_INVALID_JSON = "invalid_json";

	/** A single field-level validation problem, as reported by the API. */
	public record Detail(String field, String code, String message) {}

	private final int status;
	private final String code;
	private final List<Detail> details;
	private final String requestId;
	private final Integer retryAfter;

	public CorvoApiError(
			int status,
			String code,
			String message,
			List<Detail> details,
			String requestId,
			Integer retryAfter) {
		super(message);
		this.status = status;
		this.code = code;
		this.details = details == null ? List.of() : List.copyOf(details);
		this.requestId = requestId;
		this.retryAfter = retryAfter;
	}

	/** HTTP status code; 0 when the request never got a response (network/timeout). */
	public int status() {
		return status;
	}

	/** Stable error code from the server, or {@code network_error}/{@code timeout}/{@code http_<status>}. */
	public String code() {
		return code;
	}

	/** Field-level validation details; empty when the server reported none. */
	public List<Detail> details() {
		return details;
	}

	/** Server request id (body {@code requestId} or {@code X-Request-Id} header); {@code null} if absent. */
	public String requestId() {
		return requestId;
	}

	/** Seconds to wait before retrying (429 responses); {@code null} when not applicable. */
	public Integer retryAfter() {
		return retryAfter;
	}

	/** Network-level failure: no response was ever received. */
	public static CorvoApiError network(String message) {
		return new CorvoApiError(0, CODE_NETWORK, message, null, null, null);
	}

	/** The request exceeded the configured timeout. */
	public static CorvoApiError timeout(Duration timeout) {
		return new CorvoApiError(
				0, CODE_TIMEOUT, "Request timed out after " + timeout.toMillis() + "ms", null, null, null);
	}

	@Override
	public String toString() {
		return "CorvoApiError{status="
				+ status
				+ ", code="
				+ code
				+ ", message="
				+ getMessage()
				+ ", requestId="
				+ requestId
				+ ", details="
				+ details.size()
				+ ", retryAfter="
				+ retryAfter
				+ "}";
	}
}
