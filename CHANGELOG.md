# Changelog

## 0.3.0

Follows the API contract 0.3.0 (92 operations, `openapi.json`).

### Breaking

- `invoices().create(...)` returns `Result<InvoiceCreateResult>`: `SCHEDULE` and the new `RECURRING` answer with a schedule, not an invoice (`isSchedule()`, `schedule()`, `invoice()`).
- Plan slugs are now `free`, `solo`, `equipe`, `empresa`, `escala` (and `self-hosted`); `developer`, `growth`, `scale` and `volume` are gone. Treat the slug as an open string.
- `CreditBalance`: `overageUnits`, `overageAmountCents` and `tenantId` removed; `planRemaining` and `avulsoBalance` added; `balance` and the amounts are integers.
- `BillingPlanResponse`: `usage.overage_units` and `usage.overage_amount_cents` removed; `usage.avulso_balance`, `subscription.stripe_portal_available`, `stripe_subscription` and `pix_subscription` added. A workspace without a plan answers 404 `NO_PLAN_ASSIGNED`.
- `Invoice`, `Company`, `CreditTransaction`, `InvoiceEvent`, `Webhook`, `WebhookWithSecret` follow the contract exactly (for example `Invoice` loses `documentKeyVersion`, `Company` loses `deletedAt`, numbers that the contract types as `number` are `Double`).
- `webhooks().remove`, `webhooks().test` and `deliveries().retry` return `Deleted`, `WebhookTestResult` and `WebhookRetried` (the nested `DeletedResult`, `SentResult` and `RetryResult` are gone).
- A test key can no longer reach live webhooks (`Webhook.environment`); webhook URLs on private networks are refused by the API.

### Added

- 51 operations: `cnpj()`, `customers()`, `serviceFavorites()`, `accountantPackage()`, `invoicePackage()`, `dpsSequence()`, `fiscalProfiles()`, `serviceTaxProfiles()`, `schedules()`, `imports()` (multipart upload, template and errors CSV), `integrations()`, `fiscalPortal()`, `invoices().substitute/overview/review`, `credits().previewPurchase` (now `GET /credits/purchase/preview`).
- `invoices().list(...)` filter `customerTaxId`; `invoices().sendEmail(id, email, key)` to another address.
- Invoice request fields `purchaseOrder`, `notes`, foreign customer, `options.recurrence` (`Options.recurring`); response fields `portalFlag`, `substitutesInvoiceId`, `substitutionDescription`; event type `nfse.invoice.taker_rejected`.
- `User-Agent: corvo-nfse-java/0.3.0`; `Version` class.
- `Map` and `JsonNode` request bodies keep `null` values (to clear fields); records still drop them.
- `OperationCoverageTest` checks every operationId of the vendored `openapi.json` against the SDK.

### Changed

- Binary downloads send `Accept: */*`.
