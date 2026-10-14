# corvo-nfse

**The Java SDK for the Corvo NFS-e API — self-hosted first, result-style errors that never throw, built on the JDK HTTP client with Jackson.**

Issue, list, download, and reconcile Brazilian electronic service invoices (NFS-e) from any Java 17+ app. Every API, network, and timeout failure comes back as a typed `Result.Err` — no exception-wrapping ceremony.

## Why corvo-nfse

- **Self-hosted first.** Point it at your own Corvo instance with `baseUrl`; the default `https://api.corvo.tech` only applies when you don't.
- **Result-style errors that never throw.** Every call returns `CompletableFuture<Result<T>>` with either `Result.Ok(data)` or `Result.Err(CorvoApiError)`. The error carries the stable server `code`, HTTP `status`, `requestId`, field-level `details`, and `retryAfter`. Pattern-match it exhaustively with `switch`.
- **Minimal dependencies.** `java.net.http.HttpClient` (built-in) plus Jackson for JSON. HTTP 30s default timeout.
- **Pagination you don't have to think about.** Lists return `{ data, pagination: { limit, hasMore, nextCursor } }`. Need every page? `corvo.invoices().iterate(...)` walks them as a `Stream<T>`.

## Install

```xml
<dependency>
  <groupId>technology.corvo</groupId>
  <artifactId>corvo-nfse</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Quickstart

```java
import technology.corvo.nfse.Corvo;
import technology.corvo.nfse.transport.Result;

Corvo corvo = Corvo.builder()
    .apiKey("corvo_live_xxx")            // or set CORVO_API_KEY
    .baseUrl("https://api.corvo.tech")   // or CORVO_BASE_URL for self-hosted
    .build();

Result<Paginated<Company>> result = corvo.companies().list(10, null).join();
switch (result) {
    case Result.Ok<Paginated<Company>> ok -> {
        ok.data().data().forEach(c -> System.out.println(c.legalName()));
    }
    case Result.Err<Paginated<Company>> err -> {
        CorvoApiError e = err.error();
        System.err.println(e.code() + " " + e.status() + " " + e.requestId());
    }
}
```

All resource methods return `CompletableFuture<Result<T>>` — compose them with `thenApply`/`thenCompose`, or call `.join()` for blocking style.

### Issue an invoice

```java
InvoiceCreateParams params = InvoiceCreateParams.of(
        InvoiceCreateParams.Customer.of("Customer", "52998224725").withEmail("a@b.com"),
        InvoiceCreateParams.Service.of("010101", "3170206").withDescription("Services"),
        150.5,
        "2026-10-12")
    .withCompanyId("comp_1");

Result<Invoice> invoice = corvo.invoices().create(params, "order-42").join(); // idempotent
```

### Walk every page without thinking about cursors

```java
try (Stream<Invoice> invoices = corvo.invoices().iterate("comp_1", "authorized", null, null, null, null, null)) {
    invoices.forEach(inv -> System.out.println(inv.nfseNumber()));
}
```

**Documented exception:** `iterate(...)` throws `CorvoApiError` (unchecked) if fetching a page fails — a `Stream` has no other error channel. Every other call never throws for API/network/timeout.

### Download the PDF or XML of an authorized invoice

```java
DownloadedFile pdf = corvo.invoices().pdf(invoiceId).join() instanceof Result.Ok<DownloadedFile> ok
        ? ok.data()
        : null;
Files.write(Path.of("nfse.pdf"), pdf.content());
```

### Upload a fiscal certificate (multipart, handled for you)

```java
byte[] p12 = Files.readAllBytes(Path.of("acme.p12"));
Result<Certificate> cert = corvo.certificates().upload("comp_1", p12, "password", null).join();
```

### Verify webhook signatures

```java
boolean ok = CorvoWebhooks.verifySignature(rawBody, signatureHeader, "whsec_xxx");
```

HMAC-SHA256 over the raw body, compared in constant time.

## Error handling

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

`network_error` (status 0) means the request never reached the server; `timeout` (status 0) means it exceeded the configured timeout.

## API surface

28 API-key operations + 2 public health checks:

- **companies** — create, get, update, remove, list, iterate
- **certificates** — upload, list, remove
- **invoices** — create, get, list, iterate, events, pdf, xml, downloadUrl, sendEmail
- **webhooks** — list, create, remove, test; **deliveries** — list, iterate, stats, retry
- **credits** — balance, plans, history, iterateHistory
- **billing** — plan, usage
- **health** — live, ready (public)

## The contract (in-depth)

Nine SDKs, one behavior. This SDK implements the same analysis as every other Corvo SDK, mirrored from the canonical Node SDK.

**Auth.** Every request carries `X-API-Key` (never `Authorization: Bearer`). Keys are scoped per tenant; the base URL defaults to `https://api.corvo.tech` and is overridable for self-hosted deployments. All routes live under the `v1` prefix.

**Response envelope.** Endpoints return one of three shapes, and the SDK unwraps them idiomatically: `{ "data": ... }` (single resource), `{ "data": { "props": ... } }` (resource behind a `props` wrapper), or no envelope (health checks, raw downloads). List endpoints return `{ "data": [...], "pagination": { "limit", "hasMore", "nextCursor" } }`. A `204 No Content` maps to an empty result.

**Errors never throw.** Network failures, timeouts, non-2xx responses, and malformed JSON all resolve to a typed error carrying `status`, `code`, `message`, `details`, `requestId`, and `retryAfter` — returned alongside (or as) the result, never raised. `code` falls back to `http_<status>` when the body doesn't carry one; transport-level failures use `network_error` / `timeout` / `invalid_json`. The only operation that raises is the `iterate` helper, and only on the first page's failure.

**Pagination.** List endpoints are cursor-paginated. The SDK exposes both a `list` (single page) and an `iterate`/stream helper that walks `nextCursor` until `hasMore` is false.

**Idempotency.** Invoice create and send-email accept an idempotency key, sent as the `Idempotency-Key` header so retries are safe.

**Webhooks.** Deliveries are signed with HMAC-SHA256 over the raw body. The SDK exposes a timing-safe `verifySignature(rawBody, header, secret)`.

**Surface.** Exactly 28 operations across companies, certificates, invoices, webhooks (incl. nested deliveries), credits, and billing — plus `health/live` and `health/ready`.

## Development

```bash
mvn verify          # build + all unit tests
```

Tests run in three layers:

1. **Unit** — MockWebServer-based; path, header, query, body, and envelope assertions per resource.
2. **HMAC vector** — the shared cross-SDK signature vector, plus tamper/wrong-secret/malformed cases.
3. **Contract** (optional) — skips unless `CORVO_API_KEY` and `CORVO_BASE_URL` are set, then runs against a live Corvo instance.

## License

MIT © Corvo Technology
