# corvo-nfse

**The Java SDK for the [Corvo](https://corvo.tech) NFS-e API.** Register companies, calculate taxes, issue Brazilian service invoices (NFS-e), follow them by webhook and download the PDF and XML, from Java 17+.

- **Result-style errors.** Every call returns `CompletableFuture<Result<T>>` with either `Result.Ok(data)` or `Result.Err(CorvoApiError)`. The error carries the server's stable `code`, the HTTP `status`, the `requestId`, field-level `details` and `retryAfter`. Pattern-match it with `switch`.
- **Pagination built in.** Lists return `{ data, pagination }`, and `iterate(...)` walks every page as a `Stream<T>`.
- **Safe retries.** Issuing an invoice and sending an email accept an idempotency key.
- **Webhook signature check** in constant time.
- Built on `java.net.http.HttpClient` and Jackson. Library only, no CLI.

Full API reference: <https://corvo.tech/en/docs>.

## Install

```xml
<dependency>
  <groupId>tech.corvo</groupId>
  <artifactId>corvo-nfse</artifactId>
  <version>0.2.0</version>
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
    CompanyCreateParams.of("Empresa Exemplo Ltda", "11222333000181", "Simples Nacional", "3550308")
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

Result<Invoice> invoice = corvo.invoices().create(params, "order-42").join(); // the same key never issues twice
```

Use `Options.schedule()` with a future competence date to schedule the issuance.

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

`Webhook.WEBHOOK_EVENTS` lists the 9 events you can subscribe to. The `webhook.test` event is only sent by `webhooks().test(id)`. Deduplicate by the event id: the same event can be delivered more than once.

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

- **companies**: create, get, update, remove, list, iterate
- **certificates**: upload, list, remove
- **invoices**: create, calculate, get, list, iterate, events, pdf, xml, downloadUrl, sendEmail
- **webhooks**: list, create, remove, test; **deliveries**: list, iterate, stats, retry
- **credits**: balance, plans, history, iterateHistory
- **billing**: plan, usage
- **plans**: list, get, quote (public price catalog)
- **health**: live, ready

Changing plans and the billing portal exist only in the dashboard, so they are not in the SDK.

## Configuration

`Corvo.builder()` options: `apiKey` (or `CORVO_API_KEY`), `baseUrl` (or `CORVO_BASE_URL`, default `https://api.corvo.tech`), `prefix` (default `v1`), `timeout` (default 30s) and `allowInsecureHttp` (`http://` is only accepted for loopback and private hosts unless enabled). Keys are `corvo_test_...` or `corvo_live_...` and belong to one account and environment; the key decides the environment.

## Development

```bash
mvn verify   # build + unit tests (the contract test skips without env)
```

The contract test runs only when `CORVO_API_KEY` and `CORVO_BASE_URL` point to a reachable instance.

## License

MIT, see [LICENSE](LICENSE).
