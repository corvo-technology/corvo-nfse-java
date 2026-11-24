package technology.corvo.nfse.resources;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.CompleteCustomer;
import technology.corvo.nfse.types.CreateConnection;
import technology.corvo.nfse.types.IntegrationConnection;
import technology.corvo.nfse.types.IntegrationConnectionCreated;
import technology.corvo.nfse.types.IntegrationEvent;
import technology.corvo.nfse.types.IntegrationOperation;
import technology.corvo.nfse.types.IntegrationOperationRetry;
import technology.corvo.nfse.types.IntegrationProvider;
import technology.corvo.nfse.types.IntegrationRule;
import technology.corvo.nfse.types.IntegrationRuleInput;
import technology.corvo.nfse.types.PutRules;
import technology.corvo.nfse.types.UpdateCredentials;

/**
 * Payment-platform integrations that issue invoices from sales. Needs a key of the same
 * environment as the connection: a test key cannot reach live connections.
 */
public final class IntegrationsResource {

	private final HttpTransport http;
	private final Connections connections;
	private final Operations operations;

	public IntegrationsResource(HttpTransport http) {
		this.http = http;
		this.connections = new Connections(http);
		this.operations = new Operations(http);
	}

	/** The connections sub-resource. */
	public Connections connections() {
		return connections;
	}

	/** The operations (sales) sub-resource. */
	public Operations operations() {
		return operations;
	}

	/** Lists the payment platforms Corvo can connect to. */
	public CompletableFuture<Result<List<IntegrationProvider>>> listProviders() {
		return http.requestList(HttpMethod.GET, "/integrations/providers", null, null, IntegrationProvider.class);
	}

	/** Connections: {@code corvo.integrations().connections()}. */
	public static final class Connections {

		private static final String BASE = "/integrations/connections";

		private final HttpTransport http;

		Connections(HttpTransport http) {
			this.http = http;
		}

		/** Lists the connections. */
		public CompletableFuture<Result<List<IntegrationConnection>>> list() {
			return http.requestList(HttpMethod.GET, BASE, null, null, IntegrationConnection.class);
		}

		/** Creates a connection. The response carries the {@code secrets} the platform needs, shown only once. */
		public CompletableFuture<Result<IntegrationConnectionCreated>> create(CreateConnection params) {
			return http.request(HttpMethod.POST, BASE, params, null, null, UnwrapMode.DATA,
					IntegrationConnectionCreated.class);
		}

		/** Fetches a connection. */
		public CompletableFuture<Result<IntegrationConnection>> get(String id) {
			return http.request(HttpMethod.GET, path(id), null, null, null, UnwrapMode.DATA,
					IntegrationConnection.class);
		}

		/** Checks the credentials and identifies the external account. */
		public CompletableFuture<Result<IntegrationConnection>> validate(String id) {
			return post(id, "validate");
		}

		/** Replaces the credentials (for example a rotated API key). */
		public CompletableFuture<Result<IntegrationConnection>> updateCredentials(String id, UpdateCredentials params) {
			return http.request(HttpMethod.PUT, path(id) + "/credentials", params, null, null, UnwrapMode.DATA,
					IntegrationConnection.class);
		}

		/** Starts issuing invoices automatically. */
		public CompletableFuture<Result<IntegrationConnection>> activate(String id) {
			return post(id, "activate");
		}

		/** Stops issuing; events are still recorded. */
		public CompletableFuture<Result<IntegrationConnection>> pause(String id) {
			return post(id, "pause");
		}

		/** Disconnects for good. */
		public CompletableFuture<Result<IntegrationConnection>> disconnect(String id) {
			return post(id, "disconnect");
		}

		/** Lists the rules of a connection. */
		public CompletableFuture<Result<List<IntegrationRule>>> listRules(String id) {
			return http.requestList(HttpMethod.GET, path(id) + "/rules", null, null, IntegrationRule.class);
		}

		/** Replaces the rules of a connection (up to 50). */
		public CompletableFuture<Result<List<IntegrationRule>>> replaceRules(String id, List<IntegrationRuleInput> rules) {
			return http.requestList(HttpMethod.PUT, path(id) + "/rules", new PutRules(rules), null,
					IntegrationRule.class);
		}

		/** Lists the recent events received for a connection. */
		public CompletableFuture<Result<List<IntegrationEvent>>> listEvents(String id, Integer limit) {
			return http.requestList(HttpMethod.GET, path(id) + "/events", null, Query.of("limit", limit),
					IntegrationEvent.class);
		}

		/** Lists the recent operations (sales) and their invoice status. */
		public CompletableFuture<Result<List<IntegrationOperation>>> listOperations(String id, Integer limit) {
			return http.requestList(HttpMethod.GET, path(id) + "/operations", null, Query.of("limit", limit),
					IntegrationOperation.class);
		}

		private CompletableFuture<Result<IntegrationConnection>> post(String id, String action) {
			return http.request(HttpMethod.POST, path(id) + "/" + action, null, null, null, UnwrapMode.DATA,
					IntegrationConnection.class);
		}

		private static String path(String id) {
			return BASE + "/" + CompaniesResource.encode(id);
		}
	}

	/** Operations: {@code corvo.integrations().operations()}. */
	public static final class Operations {

		private static final String BASE = "/integrations/operations";

		private final HttpTransport http;

		Operations(HttpTransport http) {
			this.http = http;
		}

		/** Fills in the customer of an operation that is waiting for it. */
		public CompletableFuture<Result<IntegrationOperation>> completeCustomer(String id, CompleteCustomer params) {
			return http.request(HttpMethod.PATCH, BASE + "/" + CompaniesResource.encode(id) + "/customer", params,
					null, null, UnwrapMode.DATA, IntegrationOperation.class);
		}

		/** Tries to issue the invoice of an operation again. */
		public CompletableFuture<Result<IntegrationOperationRetry>> retry(String id) {
			return http.request(HttpMethod.POST, BASE + "/" + CompaniesResource.encode(id) + "/retry", null, null,
					null, UnwrapMode.DATA, IntegrationOperationRetry.class);
		}
	}
}
