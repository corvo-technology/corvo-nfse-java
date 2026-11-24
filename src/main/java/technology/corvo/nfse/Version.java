package technology.corvo.nfse;

/** The SDK version, sent in the {@code User-Agent} header. Kept equal to the one in {@code pom.xml}. */
public final class Version {

	public static final String VERSION = "0.3.0";

	/** Value of the {@code User-Agent} header. */
	public static final String USER_AGENT = "corvo-nfse-java/" + VERSION;

	private Version() {}
}
