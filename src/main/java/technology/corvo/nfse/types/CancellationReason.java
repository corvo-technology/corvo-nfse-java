package technology.corvo.nfse.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Why an NFS-e invoice is being cancelled. Serialized as the API's snake_case string ({@code
 * issue_error}, {@code service_not_provided}, {@code other}).
 */
public enum CancellationReason {
	/** The invoice was issued with an error. */
	ISSUE_ERROR("issue_error"),
	/** The service was not provided. */
	SERVICE_NOT_PROVIDED("service_not_provided"),
	/** Any other reason; describe it in {@code description}. */
	OTHER("other");

	private final String value;

	CancellationReason(String value) {
		this.value = value;
	}

	/** The wire value sent to and received from the API. */
	@JsonValue
	public String value() {
		return value;
	}

	/**
	 * Parses a wire value. Returns {@code null} for {@code null} or a value this SDK version does
	 * not know, so a newer API never breaks reading an invoice.
	 */
	@JsonCreator
	public static CancellationReason fromValue(String value) {
		for (CancellationReason reason : values()) {
			if (reason.value.equals(value)) {
				return reason;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return value;
	}
}
