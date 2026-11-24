package technology.corvo.nfse.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import technology.corvo.nfse.Corvo;
import technology.corvo.nfse.TestHarness;
import technology.corvo.nfse.types.CompanyCreateParams;
import technology.corvo.nfse.types.CompanyUpdateParams;
import technology.corvo.nfse.types.CompleteCustomer;
import technology.corvo.nfse.types.CreateConnection;
import technology.corvo.nfse.types.CreateServiceFavorite;
import technology.corvo.nfse.types.CreateServiceTaxProfile;
import technology.corvo.nfse.types.InvoiceBatchItem;
import technology.corvo.nfse.types.InvoiceCancelParams;
import technology.corvo.nfse.types.InvoiceCreateParams;
import technology.corvo.nfse.types.SetDpsSequence;
import technology.corvo.nfse.types.SubstituteInvoice;
import technology.corvo.nfse.types.UpdateCredentials;
import technology.corvo.nfse.types.UpdateCustomer;
import technology.corvo.nfse.types.UpdateImportRow;
import technology.corvo.nfse.types.UpdateSchedule;
import technology.corvo.nfse.types.UpdateServiceTaxProfile;
import technology.corvo.nfse.types.UpsertFiscalProfile;
import technology.corvo.nfse.types.WebhookCreateParams;

/**
 * Contract coverage: loads the vendored {@code openapi.json} (contract 0.3.0) and checks that every
 * {@code operationId} is implemented and that calling the SDK method sends the HTTP method and path
 * the contract declares, with the {@code x-api-key} header, the required query parameters and the
 * {@code Idempotency-Key} header on the operations that take it.
 *
 * <p>When the API changes, copy the new {@code openapi.json} to {@code src/test/resources/} and this
 * test fails on every operation that is not in the table below yet.
 */
class OperationCoverageTest extends TestHarness {

	private static final String ID = "11111111-1111-1111-1111-111111111111";
	private static final ObjectMapper JSON = new ObjectMapper();

	/** operationId to the SDK call. The string is the method the operation maps to (documentation). */
	private static final Map<String, Impl> TABLE = new LinkedHashMap<>();

	private record Impl(String sdkMethod, Function<Corvo, CompletableFuture<?>> call) {}

	private static void op(String operationId, String sdkMethod, Function<Corvo, CompletableFuture<?>> call) {
		TABLE.put(operationId, new Impl(sdkMethod, call));
	}

	private static <T> T body(String json, Class<T> type) {
		try {
			return JSON.readValue(json, type);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static InvoiceCreateParams invoice() {
		return InvoiceCreateParams.of(null, InvoiceCreateParams.Service.ofDescription("x"), 10.0, "2026-11-24")
				.withCompanyId(ID);
	}

	static {
		op("billing_getPlan", "billing().plan", c -> c.billing().plan());
		op("billing_getUsage", "billing().usage", c -> c.billing().usage());
		op("cnpj_lookup", "cnpj().lookup", c -> c.cnpj().lookup("12345678000195"));
		op("companies_list", "companies().list", c -> c.companies().list(10, "c1"));
		op("companies_create", "companies().create",
				c -> c.companies().create(CompanyCreateParams.of("Acme", "12345678000195", "MEI", "3550308")));
		op("accountantPackage_get", "accountantPackage().get", c -> c.accountantPackage().get(ID));
		op("accountantPackage_update", "accountantPackage().update",
				c -> c.accountantPackage().update(ID, "contador@example.com"));
		op("accountantPackage_send", "accountantPackage().send", c -> c.accountantPackage().send(ID, "2026-10"));
		op("accountantPackage_listSends", "accountantPackage().listSends", c -> c.accountantPackage().listSends(ID));
		op("certificates_list", "certificates().list", c -> c.certificates().list(ID, 10, "c1"));
		op("certificates_upload", "certificates().upload",
				c -> c.certificates().upload(ID, new byte[] {1, 2, 3}, "pw", "cert.p12"));
		op("certificates_revoke", "certificates().remove", c -> c.certificates().remove(ID, ID));
		op("invoicePackage_download", "invoicePackage().download",
				c -> c.invoicePackage().download(ID, "2026-10", "xml,pdf"));
		op("invoicePackage_summary", "invoicePackage().summary", c -> c.invoicePackage().summary(ID, "2026-10"));
		op("serviceFavorites_list", "serviceFavorites().list", c -> c.serviceFavorites().list(ID));
		op("serviceFavorites_create", "serviceFavorites().create",
				c -> c.serviceFavorites().create(ID,
						body("{\"label\":\"Mensalidade\",\"description\":\"d\",\"serviceCode\":\"010101\"}",
								CreateServiceFavorite.class)));
		op("serviceFavorites_delete", "serviceFavorites().remove", c -> c.serviceFavorites().remove(ID, ID));
		op("companies_delete", "companies().remove", c -> c.companies().remove(ID));
		op("companies_get", "companies().get", c -> c.companies().get(ID));
		op("companies_update", "companies().update",
				c -> c.companies().update(ID, body("{\"tradeName\":\"x\"}", CompanyUpdateParams.class)));
		op("dpsSequence_get", "dpsSequence().get", c -> c.dpsSequence().get(ID));
		op("dpsSequence_set", "dpsSequence().set", c -> c.dpsSequence().set(ID, new SetDpsSequence(120.0)));
		op("fiscalProfiles_list", "fiscalProfiles().list", c -> c.fiscalProfiles().list(ID));
		op("fiscalProfiles_upsert", "fiscalProfiles().upsert",
				c -> c.fiscalProfiles().upsert(ID, body("{}", UpsertFiscalProfile.class)));
		op("fiscalProfiles_approve", "fiscalProfiles().approve", c -> c.fiscalProfiles().approve(ID, ID));
		op("serviceTaxProfiles_list", "serviceTaxProfiles().list",
				c -> c.serviceTaxProfiles().list(ID, "2026-11-24"));
		op("serviceTaxProfiles_create", "serviceTaxProfiles().create",
				c -> c.serviceTaxProfiles().create(ID, body("{}", CreateServiceTaxProfile.class)));
		op("serviceTaxProfiles_delete", "serviceTaxProfiles().remove", c -> c.serviceTaxProfiles().remove(ID, ID));
		op("serviceTaxProfiles_update", "serviceTaxProfiles().update",
				c -> c.serviceTaxProfiles().update(ID, ID, body("{}", UpdateServiceTaxProfile.class)));
		op("serviceTaxProfiles_approve", "serviceTaxProfiles().approve", c -> c.serviceTaxProfiles().approve(ID, ID));
		op("credits_getBalance", "credits().balance", c -> c.credits().balance());
		op("credits_listHistory", "credits().history", c -> c.credits().history(10, "c1"));
		op("credits_listPlans", "credits().plans", c -> c.credits().plans(10, "c1"));
		op("credits_previewPurchase", "credits().previewPurchase", c -> c.credits().previewPurchase(10));
		op("customers_search", "customers().search", c -> c.customers().search("maria", 10, "c1"));
		op("customers_delete", "customers().remove", c -> c.customers().remove("12345678909"));
		op("customers_get", "customers().get", c -> c.customers().get("12345678909"));
		op("customers_update", "customers().update",
				c -> c.customers().update("12345678909", body("{\"name\":\"x\"}", UpdateCustomer.class)));
		op("fiscalPortal_status", "fiscalPortal().status", c -> c.fiscalPortal().status());
		op("health_live", "health().live", c -> c.health().live());
		op("imports_list", "imports().list", c -> c.imports().list(5));
		op("imports_start", "imports().start", c -> c.imports().start(new byte[] {1}, "notas.csv", ID));
		op("imports_discard", "imports().discard", c -> c.imports().discard(ID));
		op("imports_get", "imports().get", c -> c.imports().get(ID));
		op("imports_emit", "imports().emit", c -> c.imports().emit(ID, true));
		op("imports_downloadErrors", "imports().downloadErrors", c -> c.imports().downloadErrors(ID));
		op("imports_listRows", "imports().listRows", c -> c.imports().listRows(ID, "invalid", 10, "20"));
		op("imports_updateRow", "imports().updateRow",
				c -> c.imports().updateRow(ID, 3, body("{\"item\":{\"amount\":10}}", UpdateImportRow.class)));
		op("imports_downloadTemplate", "imports().downloadTemplate", c -> c.imports().downloadTemplate());
		op("integrationConnections_list", "integrations().connections().list",
				c -> c.integrations().connections().list());
		op("integrationConnections_create", "integrations().connections().create",
				c -> c.integrations().connections().create(body(
						"{\"provider\":\"stripe\",\"companyId\":\"" + ID + "\",\"credentials\":{\"apiKey\":\"k\"}}",
						CreateConnection.class)));
		op("integrationConnections_get", "integrations().connections().get",
				c -> c.integrations().connections().get(ID));
		op("integrationConnections_activate", "integrations().connections().activate",
				c -> c.integrations().connections().activate(ID));
		op("integrationConnections_updateCredentials", "integrations().connections().updateCredentials",
				c -> c.integrations().connections().updateCredentials(ID,
						body("{\"credentials\":{\"apiKey\":\"k2\"}}", UpdateCredentials.class)));
		op("integrationConnections_disconnect", "integrations().connections().disconnect",
				c -> c.integrations().connections().disconnect(ID));
		op("integrationConnections_listEvents", "integrations().connections().listEvents",
				c -> c.integrations().connections().listEvents(ID, 10));
		op("integrationConnections_listOperations", "integrations().connections().listOperations",
				c -> c.integrations().connections().listOperations(ID, 10));
		op("integrationConnections_pause", "integrations().connections().pause",
				c -> c.integrations().connections().pause(ID));
		op("integrationConnections_listRules", "integrations().connections().listRules",
				c -> c.integrations().connections().listRules(ID));
		op("integrationConnections_replaceRules", "integrations().connections().replaceRules",
				c -> c.integrations().connections().replaceRules(ID, List.of()));
		op("integrationConnections_validate", "integrations().connections().validate",
				c -> c.integrations().connections().validate(ID));
		op("integrationOperations_completeCustomer", "integrations().operations().completeCustomer",
				c -> c.integrations().operations().completeCustomer(ID,
						body("{\"taxId\":\"12345678909\",\"name\":\"x\"}", CompleteCustomer.class)));
		op("integrationOperations_retry", "integrations().operations().retry",
				c -> c.integrations().operations().retry(ID));
		op("integrations_listProviders", "integrations().listProviders", c -> c.integrations().listProviders());
		op("invoices_list", "invoices().list",
				c -> c.invoices().list(ID, "authorized", "2026-11-01", "2026-11-30", "ext-1", "12345678909", 10, "c1"));
		op("invoices_create", "invoices().create", c -> c.invoices().create(invoice(), "key-1"));
		op("invoices_get", "invoices().get", c -> c.invoices().get(ID));
		op("invoices_cancel", "invoices().cancel",
				c -> c.invoices().cancel(ID, InvoiceCancelParams.of("other", "a reason long enough")));
		op("invoices_createDownloadUrl", "invoices().downloadUrl", c -> c.invoices().downloadUrl(ID, "pdf"));
		op("invoices_sendEmail", "invoices().sendEmail",
				c -> c.invoices().sendEmail(ID, "outro@example.com", "key-1"));
		op("invoices_listEvents", "invoices().events", c -> c.invoices().events(ID, 10, "c1"));
		op("invoices_downloadPdf", "invoices().pdf", c -> c.invoices().pdf(ID));
		op("invoices_substitute", "invoices().substitute",
				c -> c.invoices().substitute(ID, body("{\"description\":\"corrigir\"}", SubstituteInvoice.class), "key-1"));
		op("invoices_downloadXml", "invoices().xml", c -> c.invoices().xml(ID));
		op("invoices_createBatch", "invoices().createBatch",
				c -> c.invoices().createBatch(List.of(InvoiceBatchItem.of(invoice(), "row-1"))));
		op("invoices_calculate", "invoices().calculate", c -> c.invoices().calculate(invoice()));
		op("invoices_overview", "invoices().overview", c -> c.invoices().overview("30d"));
		op("invoices_review", "invoices().review", c -> c.invoices().review("retry", List.of(ID)));
		op("schedules_list", "schedules().list", c -> c.schedules().list("active", ID, 10, "20"));
		op("schedules_get", "schedules().get", c -> c.schedules().get(ID));
		op("schedules_update", "schedules().update",
				c -> c.schedules().update(ID, body("{\"endsOn\":\"2027-01-01\"}", UpdateSchedule.class)));
		op("schedules_cancel", "schedules().cancel", c -> c.schedules().cancel(ID));
		op("schedules_pause", "schedules().pause", c -> c.schedules().pause(ID));
		op("schedules_resume", "schedules().resume", c -> c.schedules().resume(ID));
		op("schedules_listRuns", "schedules().listRuns", c -> c.schedules().listRuns(ID, 10, "20"));
		op("webhooks_list", "webhooks().list", c -> c.webhooks().list(10, "c1"));
		op("webhooks_create", "webhooks().create",
				c -> c.webhooks().create(WebhookCreateParams.of("https://example.com/hook", List.of("nfse.invoice.authorized"))));
		op("webhooks_delete", "webhooks().remove", c -> c.webhooks().remove(ID));
		op("webhooks_test", "webhooks().test", c -> c.webhooks().test(ID));
		op("webhookDeliveries_list", "webhooks().deliveries().list", c -> c.webhooks().deliveries().list(24, "test", 10, "c1"));
		op("webhookDeliveries_retry", "webhooks().deliveries().retry", c -> c.webhooks().deliveries().retry(ID));
		op("webhookDeliveries_stats", "webhooks().deliveries().stats", c -> c.webhooks().deliveries().stats(24, "test"));
	}

	private static JsonNode spec() throws IOException {
		try (InputStream in = OperationCoverageTest.class.getResourceAsStream("/openapi.json")) {
			return JSON.readTree(in);
		}
	}

	private record Operation(String id, String method, String path, JsonNode node) {}

	private static List<Operation> operations() throws IOException {
		List<Operation> ops = new ArrayList<>();
		spec().get("paths").fields().forEachRemaining(path ->
				path.getValue().fields().forEachRemaining(m -> {
					if (m.getValue().has("operationId")) {
						ops.add(new Operation(m.getValue().get("operationId").asText(), m.getKey().toUpperCase(),
								path.getKey(), m.getValue()));
					}
				}));
		return ops;
	}

	@Test
	void theContractIsVersion030With92Operations() throws Exception {
		assertThat(spec().get("info").get("version").asText()).isEqualTo("0.3.0");
		assertThat(operations()).hasSize(92);
	}

	@Test
	void everyOperationOfTheContractIsImplementedAndNothingIsStale() throws Exception {
		TreeSet<String> contract = new TreeSet<>();
		operations().forEach(o -> contract.add(o.id()));
		assertThat(new TreeSet<>(TABLE.keySet()))
				.as("operationIds in the SDK table vs the contract")
				.isEqualTo(contract);
	}

	@Test
	void everyOperationSendsTheMethodAndPathOfTheContract() throws Exception {
		Corvo corvo = Corvo.builder().apiKey(API_KEY).baseUrl(server.url("/").toString()).build();
		for (Operation op : operations()) {
			enqueueFor(op);
			Impl impl = TABLE.get(op.id());
			impl.call().apply(corvo).get(5, TimeUnit.SECONDS);
			RecordedRequest request = takeRequest();

			StringBuilder rx = new StringBuilder("^/v1");
			for (String part : op.path().split("(?<=})|(?=\\{)")) {
				rx.append(part.startsWith("{") ? "[^/]+" : Pattern.quote(part));
			}
			String regex = rx + "(\\?.*)?$";
			assertThat(request.getMethod()).as(op.id() + " method").isEqualTo(op.method());
			assertThat(request.getPath()).as(op.id() + " path").matches(regex);
			assertThat(request.getHeader("x-api-key")).as(op.id() + " x-api-key").isEqualTo(API_KEY);
			assertThat(request.getHeader("User-Agent")).as(op.id() + " user-agent").startsWith("corvo-nfse-java/");
			for (JsonNode param : op.node().path("parameters")) {
				if ("query".equals(param.path("in").asText()) && param.path("required").asBoolean()) {
					String name = param.get("name").asText();
					assertThat(request.getRequestUrl().queryParameter(name)).as(op.id() + " ?" + name).isNotNull();
				}
				if (param.path("$ref").asText().endsWith("/IdempotencyKey")) {
					assertThat(request.getHeader("Idempotency-Key")).as(op.id() + " Idempotency-Key").isEqualTo("key-1");
				}
			}
		}
	}

	private void enqueueFor(Operation op) {
		JsonNode responses = op.node().get("responses");
		int status = 200;
		JsonNode success = null;
		for (var it = responses.fields(); it.hasNext(); ) {
			var e = it.next();
			if (e.getKey().startsWith("2")) {
				status = Integer.parseInt(e.getKey());
				success = e.getValue();
				break;
			}
		}
		JsonNode content = success == null ? null : success.path("content");
		if (content == null || content.isMissingNode() || content.size() == 0) {
			server.enqueue(new MockResponse().setResponseCode(status));
			return;
		}
		String type = content.fieldNames().next();
		if (!type.equals("application/json")) {
			server.enqueue(new MockResponse().setResponseCode(status).setHeader("Content-Type", type).setBody("data"));
			return;
		}
		JsonNode schema = content.get(type).path("schema");
		boolean enveloped = schema.path("properties").has("data");
		boolean list = schema.path("properties").path("data").path("type").asText().equals("array");
		String body;
		if (!enveloped) {
			// the API wraps every JSON answer in {data}, health aside (the controllers win over the contract)
			body = op.id().equals("invoices_calculate") ? "{\"data\":{}}" : "{}";
		} else if (list) {
			body = "{\"data\":[],\"pagination\":{\"limit\":50,\"hasMore\":false,\"nextCursor\":null}}";
		} else {
			body = "{\"data\":{}}";
		}
		enqueueJson(status, body);
	}
}
