package technology.corvo.nfse.transport;

/**
 * How a success response body is unwrapped:
 *
 * <ul>
 *   <li>{@link #DATA} — return {@code body.data} (or {@code null} if missing).
 *   <li>{@link #DATA_PROPS} — return {@code body.data.props}, unwrapping the domain entity wrapper
 *       used by {@code Invoice}, {@code InvoiceEvent}, {@code CreditPlan}, and
 *       {@code CreditTransaction}.
 *   <li>{@link #NONE} — return the whole body (used for list pages and raw health checks).
 * </ul>
 */
public enum UnwrapMode {
	DATA,
	DATA_PROPS,
	NONE
}
