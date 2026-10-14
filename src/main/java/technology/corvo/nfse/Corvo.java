package technology.corvo.nfse;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Locale;
import technology.corvo.nfse.resources.BillingResource;
import technology.corvo.nfse.resources.CertificatesResource;
import technology.corvo.nfse.resources.CompaniesResource;
import technology.corvo.nfse.resources.CreditsResource;
import technology.corvo.nfse.resources.HealthResource;
import technology.corvo.nfse.resources.InvoicesResource;
import technology.corvo.nfse.resources.WebhooksResource;
import technology.corvo.nfse.transport.HttpTransport;

/**
 * Entry point for the Corvo NFS-e API.
 *
 * <pre>{@code
 * Corvo corvo = Corvo.builder()
 *     .apiKey("corvo_live_xxx")            // or set CORVO_API_KEY
 *     .baseUrl("https://api.corvo.tech")   // or CORVO_BASE_URL for self-hosted
 *     .build();
 *
 * Result<Paginated<Company>> result = corvo.companies().list(10, null).join();
 * }</pre>
 *
 * <p>Construction validates its configuration and throws {@link IllegalArgumentException} on
 * misuse (missing API key, {@code http://} on a public host, unparseable base URL). Once built,
 * no API, network, or timeout failure throws — results arrive as {@code Result.Ok}/{@code Result.Err}.
 */
public final class Corvo {

	public static final String DEFAULT_BASE_URL = "https://api.corvo.tech";
	public static final String DEFAULT_PREFIX = "v1";

	private final CompaniesResource companies;
	private final CertificatesResource certificates;
	private final InvoicesResource invoices;
	private final WebhooksResource webhooks;
	private final CreditsResource credits;
	private final BillingResource billing;
	private final HealthResource health;

	private Corvo(HttpTransport http) {
		this.companies = new CompaniesResource(http);
		this.certificates = new CertificatesResource(http);
		this.invoices = new InvoicesResource(http);
		this.webhooks = new WebhooksResource(http);
		this.credits = new CreditsResource(http);
		this.billing = new BillingResource(http);
		this.health = new HealthResource(http);
	}

	/** Companies (emitters): create, get, update, remove, list, iterate. */
	public CompaniesResource companies() {
		return companies;
	}

	/** Fiscal certificates: upload, list, remove. */
	public CertificatesResource certificates() {
		return certificates;
	}

	/** Invoices: create, get, list, iterate, events, pdf, xml, downloadUrl, sendEmail. */
	public InvoicesResource invoices() {
		return invoices;
	}

	/** Webhooks: list, create, remove, test, and the {@code deliveries()} sub-resource. */
	public WebhooksResource webhooks() {
		return webhooks;
	}

	/** Credits: balance, plans, history, iterateHistory. */
	public CreditsResource credits() {
		return credits;
	}

	/** Billing: plan, usage. */
	public BillingResource billing() {
		return billing;
	}

	/** Health: live, ready (public endpoints). */
	public HealthResource health() {
		return health;
	}

	public static Builder builder() {
		return new Builder();
	}

	/** Fluent builder for {@link Corvo}. */
	public static final class Builder {

		private String apiKey;
		private String baseUrl;
		private String prefix = DEFAULT_PREFIX;
		private Duration timeout = HttpTransport.DEFAULT_TIMEOUT;
		private boolean allowInsecureHttp;

		private Builder() {}

		/** API key ({@code corvo_test_}/{@code corvo_live_} + 40 hex). Falls back to {@code CORVO_API_KEY}. */
		public Builder apiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		/** Base URL of the Corvo API. Falls back to {@code CORVO_BASE_URL}, then the public default. */
		public Builder baseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
			return this;
		}

		/** API prefix for self-hosted installs with a custom {@code API_PREFIX}; default {@code v1}. */
		public Builder prefix(String prefix) {
			this.prefix = prefix;
			return this;
		}

		/** Per-request timeout; default 30s. */
		public Builder timeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		/** Allow {@code http://} on public hosts (loopback/private hosts are always allowed). */
		public Builder allowInsecureHttp(boolean allowInsecureHttp) {
			this.allowInsecureHttp = allowInsecureHttp;
			return this;
		}

		public Corvo build() {
			String effectiveApiKey = firstNonEmpty(apiKey, System.getenv("CORVO_API_KEY"));
			if (effectiveApiKey == null) {
				throw new IllegalArgumentException(
						"Corvo: apiKey is required — pass apiKey or set CORVO_API_KEY");
			}
			String effectiveBaseUrl = firstNonEmpty(baseUrl, System.getenv("CORVO_BASE_URL"), DEFAULT_BASE_URL);
			validateBaseUrl(effectiveBaseUrl, allowInsecureHttp);
			HttpTransport http =
					new HttpTransport(effectiveApiKey, effectiveBaseUrl, prefix, timeout);
			return new Corvo(http);
		}

		private static void validateBaseUrl(String baseUrl, boolean allowInsecureHttp) {
			URI uri;
			try {
				uri = new URI(baseUrl);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Corvo: invalid baseUrl: " + baseUrl, e);
			}
			String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			if (!scheme.equals("http") && !scheme.equals("https")) {
				throw new IllegalArgumentException("Corvo: invalid baseUrl (must be http/https): " + baseUrl);
			}
			String host = uri.getHost() == null ? "" : uri.getHost();
			if (scheme.equals("http") && !allowInsecureHttp && !isPrivateHost(host)) {
				throw new IllegalArgumentException(
						"Corvo: http:// is only allowed for loopback/private hosts (got " + host
								+ "). Use https:// or pass allowInsecureHttp(true).");
			}
		}

		/**
		 * Mirrors the Node SDK's private-host check: loopback names, and RFC1918 / link-local
		 * IPv4 ranges.
		 */
		static boolean isPrivateHost(String hostname) {
			String host = hostname.toLowerCase(Locale.ROOT);
			if (host.equals("localhost")
					|| host.equals("[::1]")
					|| host.equals("::1")
					|| host.endsWith(".localhost")) {
				return true;
			}
			String[] parts = host.split("\\.", -1);
			if (parts.length != 4) {
				return false;
			}
			int a;
			int b;
			try {
				a = Integer.parseInt(parts[0]);
				b = Integer.parseInt(parts[1]);
				Integer.parseInt(parts[2]);
				Integer.parseInt(parts[3]);
			} catch (NumberFormatException e) {
				return false;
			}
			return a == 127
					|| a == 10
					|| a == 0
					|| (a == 192 && b == 168)
					|| (a == 172 && b >= 16 && b <= 31)
					|| (a == 169 && b == 254);
		}

		private static String firstNonEmpty(String... values) {
			for (String value : values) {
				if (value != null && !value.isEmpty()) {
					return value;
				}
			}
			return null;
		}
	}
}
