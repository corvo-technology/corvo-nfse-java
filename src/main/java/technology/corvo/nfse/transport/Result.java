package technology.corvo.nfse.transport;

import technology.corvo.nfse.error.CorvoApiError;

/**
 * Result-style outcome of a Corvo API call. API, network, and timeout failures never throw —
 * they are returned as {@link Err}.
 *
 * <pre>{@code
 * Result<Company> result = corvo.companies().getAsync("comp_1").join();
 * switch (result) {
 *   case Ok<Company> ok -> System.out.println(ok.data().legalName());
 *   case Err<Company> err -> System.err.println(err.error().code());
 * }
 * }</pre>
 *
 * @param <T> the success payload type
 */
public sealed interface Result<T> permits Result.Ok, Result.Err {

	/** Successful outcome carrying the unwrapped payload ({@code null} for 204/empty responses). */
	record Ok<T>(T data) implements Result<T> {}

	/** Failed outcome carrying the typed {@link CorvoApiError}. */
	record Err<T>(CorvoApiError error) implements Result<T> {}

	static <T> Result<T> ok(T data) {
		return new Ok<>(data);
	}

	static <T> Result<T> err(CorvoApiError error) {
		return new Err<>(error);
	}

	default boolean isOk() {
		return this instanceof Ok;
	}

	default boolean isErr() {
		return this instanceof Err;
	}
}
