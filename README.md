# corvo-nfse

**The Java SDK for the [Corvo](https://corvo.tech) NFS-e API.** Register companies, calculate taxes, issue Brazilian service invoices (NFS-e), follow them by webhook and download the PDF and XML, from Java 17+.

- **Result-style errors.** Every call returns `CompletableFuture<Result<T>>` with either `Result.Ok(data)` or `Result.Err(CorvoApiError)`. The error carries the server's stable `code`, the HTTP `status`, the `requestId`, field-level `details` and `retryAfter`. Pattern-match it with `switch`.
- **Pagination built in.** Lists return `{ data, pagination }`, and `iterate(...)` walks every page as a `Stream<T>`.
- **Safe retries.** Issuing an invoice and sending an email accept an idempotency key.
- **Webhook signature check** in constant time.
- Built on `java.net.http.HttpClient` and Jackson. Library only, no CLI.

Full API reference: <https://corvo.tech/en/docs>. OpenAPI contract (0.3.0, 92 operations): <https://corvo.tech/openapi.json>.

## Install

```xml
<dependency>
  <groupId>tech.corvo</groupId>
  <artifactId>corvo-nfse</artifactId>
  <version>0.3.0</version>
</dependency>
```

## Quickstart

Start with a `corvo_test_...` key: invoices issued with it have no fiscal value.

```java
import technology.corvo.nfse.Corvo;
import technology.corvo.nfse.transport.Result;

Corvo corvo = Corvo.builder()
    .apiKey("corvo_test_...")            // or set CORVO_API_KEY
    // .baseUrl("https://api.corvo.tech") // default; also read from CORVO_BASE_URL
    .build();

Result<Paginated<Company>> result = corvo.companies().list(10, null).join();
switch (result) {
    case Result.Ok<Paginated<Company>> ok ->
        ok.data().data().forEach(c -> System.out.println(c.legalName()));
    case Result.Err<Paginated<Company>> err -> {
        CorvoApiError e = err.error();
        System.err.println(e.code() + " " + e.status() + " " + e.requestId());
    }
}
```

`Corvo.builder().build()` is the only call that throws (`IllegalArgumentException`): it fails fast when no API key is configured or the base URL is invalid. All resource methods return `CompletableFuture<Result<T>>`: compose them with `thenApply`/`thenCompose`, or call `.join()` for blocking style.

### Register a company

```java
CompanyCreateParams params =
    CompanyCreateParams.of("Empresa Exemplo Ltda", "11222333000181", "Simples Nacional", "3550308") // "MEI" or "Simples Nacional": the only regimes Corvo serves
        .withSimplesRate("6.54") // required for Simples Nacional: effective DAS rate (%), ignored for MEI
        .withDefaultServiceCode("010101"); // optional: used when an invoice omits service.code

Result<Company> company = corvo.companies().create(params).join();
```

In `live`, also upload the A1 certificate (below). The company is ready when `status()` is `"ready"` and `setupGaps()` is empty.

### Estimate the taxes before issuing

Nothing is issued, stored or charged.

```java
InvoiceCreateParams params = InvoiceCreateParams.of(
        null,                                              // no customer
        InvoiceCreateParams.Service.ofDescription("Consultoria"),
        1500.0,
        "2026-10-16")
    .withCompanyTaxId("11222333000181");

Result<InvoiceCalculation> estimate = corvo.invoices().calculate(params).join();
// status() is "ready" or "blocked": a fiscal gap is data, not an error.
```

### Issue an invoice

Issuing is asynchronous: a successful call means the request was accepted, not that the invoice is authorized. Follow the status with `invoices().get(id)` or webhooks.

```java
InvoiceCreateParams params = InvoiceCreateParams.of(
        // The customer is optional; when sent, taxId (CPF or CNPJ) is required.
        InvoiceCreateParams.Customer.of("Cliente", "52998224725").withEmail("cliente@example.com"),
        InvoiceCreateParams.Service.ofDescription("Consultoria"), // code and city default to the company's
        150.5,
        "2026-10-16")
    .withCompanyTaxId("11222333000181") // or withCompanyId(...)
    .withOptions(InvoiceCreateParams.Options.sendEmail(true));

Result<InvoiceCreateResult> created = corvo.invoices().create(params, "order-42").join(); // the same key never issues twice
if (created instanceof Result.Ok<InvoiceCreateResult> ok) {
    Invoice invoice = ok.data().invoice(); // null when a schedule was created (see below)
}
```

`purchaseOrder` and `notes` (`.withPurchaseOrder(...)`, `.withNotes(...)`) print on the invoice and change no tax. A customer outside Brazil uses `Customer.ofForeign(name, foreign)`.

### Schedule or repeat an invoice

`Options.schedule()` and `Options.recurring("MONTHLY", endsOn)` do not issue now: the answer is the **schedule** (`result.schedule()`, `isSchedule()` is true), and the note exists only when it runs (09:00 America/Sao_Paulo, Monday to Friday). `competenceDate` is the date of the first run, from tomorrow on.

```java
InvoiceCreateParams monthly = params.withOptions(InvoiceCreateParams.Options.recurring("MONTHLY", "2027-12-01"));
Schedule schedule = ((Result.Ok<InvoiceCreateResult>) corvo.invoices().create(monthly).join()).data().schedule();
corvo.schedules().listRuns(schedule.id(), null, null).join(); // follow the runs
corvo.schedules().pause(schedule.id()).join();                // also resume, cancel, update
```

Schedules and imports answer in `snake_case`; the records map those names.

### Look up a CNPJ, manage customers and favorite services

```java
CnpjProfile profile = ((Result.Ok<CnpjProfile>) corvo.cnpj().lookup("11222333000181").join()).data();
corvo.customers().search("maria", 20, null).join();   // also iterate, get, update, remove
corvo.serviceFavorites().list(companyId).join();       // also create, remove
```

To clear a field on an update (for example a customer's email), pass a `Map` with a `null` value: `Map` and `JsonNode` bodies are sent as they are, records drop their `null` fields.

### Send the monthly package to the accountant

```java
corvo.accountantPackage().update(companyId, "contador@example.com").join(); // null turns it off
corvo.accountantPackage().send(companyId, "2026-10").join();
corvo.invoicePackage().download(companyId, "2026-10", null).join();          // ZIP with xml/, pdf/ and resumo.csv
```

### Import a spreadsheet

```java
ImportStart started = ((Result.Ok<ImportStart>) corvo.imports()
    .start(Files.readAllBytes(Path.of("notas.csv")), "notas.csv", companyId).join()).data();
corvo.imports().listRows(started.id(), "invalid", 50, null).join(); // fix rows with updateRow, then:
corvo.imports().emit(started.id(), true).join();
```

`downloadTemplate()` and `downloadErrors(id)` return the CSV files.

To declare a deduction from the ISS base, use `.withDeduction(...)` (greater than 0 and less than `amount` minus `discount`). The invoice is still issued for the full amount, and you are responsible for the declared value.

### Issue up to 50 invoices at once

`createBatch` takes 1 to 50 items (they can belong to different companies). Items are checked one by one: valid ones are saved together, invalid ones come back `rejected` and are not saved. Set an idempotency key per item, so repeating the batch after a timeout returns the saved ones with `replayed`.

```java
Result<InvoiceBatchSummary> batch = corvo.invoices().createBatch(List.of(
        InvoiceBatchItem.of(params.withExternalId("row-2"), "sheet-row-2"),
        InvoiceBatchItem.of(params.withExternalId("row-3"), "sheet-row-3"))).join();

if (batch instanceof Result.Ok<InvoiceBatchSummary> ok) {
    for (InvoiceBatchSummary.Result item : ok.data().results()) {
        if ("rejected".equals(item.status())) {
            System.out.println(item.index() + " " + item.error().code() + " " + item.error().message());
        }
    }
}
```

The route allows 10 batches per minute per tenant (`429` with `Retry-After`).

### Cancel an invoice

```java
Result<Invoice> cancelled = corvo.invoices()
        .cancel(invoiceId, InvoiceCancelParams.of("issue_error", "Wrong amount on the invoice"))
        .join(); // reason: "issue_error" | "service_not_provided" | "other"
```

- `queued`: cancelled at once, `status()` is `"cancelled"` with `cancelledAt()` set.
- `authorized`: `status()` is `"cancelling"` while the cancellation is processed at the Sefin. The issuance credit is not refunded.
- `processing`: stays `"processing"` with `cancellationRequestedAt()` set and is cancelled once authorized.

The description (15 to 255 characters) is required for an authorized or processing invoice and optional for a queued one. Read the invoice again (or listen to the `nfse.invoice.cancelled` and `nfse.invoice.cancellation_rejected` webhooks) to see the outcome: `cancelled`, or back to `authorized` with `lastErrorCode()` when the cancellation was rejected.

### Walk every page

```java
try (Stream<Invoice> invoices = corvo.invoices().iterate(null, "authorized", null, null, null, null, null)) {
    invoices.forEach(inv -> System.out.println(inv.nfseNumber()));
}
```

`iterate(...)` throws `CorvoApiError` (unchecked) if a page fails, since a `Stream` has no other error channel.

### Download the PDF or XML

The invoice must be `authorized` and have its documents ready.

```java
if (corvo.invoices().pdf(invoiceId).join() instanceof Result.Ok<DownloadedFile> ok) {
    Files.write(Path.of("nfse.pdf"), ok.data().content());
}
```

### Upload a certificate

```java
byte[] p12 = Files.readAllBytes(Path.of("certificado.p12"));
Result<Certificate> cert = corvo.certificates().upload("comp_1", p12, "senha", null).join();
```

### Verify webhook signatures

```java
// rawBody: the request body bytes exactly as received; the header is X-Corvo-Signature.
boolean ok = CorvoWebhooks.verifySignature(rawBody, signatureHeader, "whsec_xxx");
```

`Webhook.WEBHOOK_EVENTS` lists the 12 events you can subscribe to. The `webhook.test` event is only sent by `webhooks().test(id)`. Deduplicate by the event id: the same event can be delivered more than once.

## Errors

```java
Result<Company> result = corvo.companies().get("comp_1").join();
if (result instanceof Result.Err<Company> err) {
    CorvoApiError e = err.error();
    switch (e.status()) {
        case 400 -> e.details().forEach(d -> System.err.println(d.field() + ": " + d.message()));
        case 429 -> System.err.println("Rate limited, retry after " + e.retryAfter() + "s");
        default -> System.err.println(e.code() + ": " + e.getMessage());
    }
}
```

`code` is the API's `ERROR_*` code, falling back to `http_<status>`. `network_error` (status 0) means the request never reached the server; `timeout` (status 0) means it exceeded the configured timeout.

## Resources

All 92 operations of the contract are covered (`OperationCoverageTest` checks it against `src/test/resources/openapi.json`).

- **companies**: create, get, update, remove, list, iterate
- **certificates**: upload, list, remove
- **invoices**: create, createBatch, calculate, get, list, iterate, events, pdf, xml, downloadUrl, sendEmail, cancel, substitute, overview, review
- **schedules**: list, iterate, get, update, pause, resume, cancel, listRuns, iterateRuns
- **imports**: start, list, get, emit, listRows, iterateRows, updateRow, discard, downloadTemplate, downloadErrors
- **customers**: search, iterate, get, update, remove; **cnpj**: lookup; **serviceFavorites**: list, create, remove
- **accountantPackage**: get, update, send, listSends; **invoicePackage**: summary, download
- **dpsSequence**: get, set; **fiscalProfiles**: list, upsert, approve; **serviceTaxProfiles**: list, create, update, remove, approve
- **integrations**: listProviders; `connections()` (list, create, get, validate, updateCredentials, activate, pause, disconnect, listRules, replaceRules, listEvents, listOperations); `operations()` (completeCustomer, retry)
- **fiscalPortal**: status
- **webhooks**: list, create, remove, test; **deliveries**: list, iterate, stats, retry (a test key cannot reach live webhooks; URLs on private networks are refused)
- **credits**: balance (`planRemaining` + `avulsoBalance`), plans, history, iterateHistory, previewPurchase
- **billing**: plan, usage
- **plans**: list, get, quote (public price catalog)
- **health**: live, ready

Changing plans and the billing portal exist only in the dashboard, so they are not in the SDK.

## Configuration

`Corvo.builder()` options: `apiKey` (or `CORVO_API_KEY`), `baseUrl` (or `CORVO_BASE_URL`, default `https://api.corvo.tech`), `prefix` (default `v1`), `timeout` (default 30s) and `allowInsecureHttp` (`http://` is only accepted for loopback and private hosts unless enabled). Keys are `corvo_test_...` or `corvo_live_...` and belong to one account and environment; the key decides the environment.

## Development

```bash
mvn verify   # build + unit tests (the live contract test skips without env)
```

To update for a new API version, copy the new `openapi.json` to `src/test/resources/`: `OperationCoverageTest` fails for each operation not yet implemented.

The live contract test runs only when `CORVO_API_KEY` and `CORVO_BASE_URL` point to a reachable instance.

## License

MIT, see [LICENSE](LICENSE).
