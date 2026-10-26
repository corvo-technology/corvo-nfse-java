package technology.corvo.nfse.types;

/**
 * Input for {@code invoices.create} and {@code invoices.calculate}. Only {@code service}, {@code
 * amount} and {@code competenceDate} are required; identify the company with {@code companyId} or
 * {@code companyTaxId}.
 *
 * @param customer optional: leave it {@code null} for an invoice without a customer
 * @param amount in BRL, positive, up to 2 decimal places
 * @param competenceDate {@code YYYY-MM-DD}; a future date needs {@code options.type = "SCHEDULE"}
 * @param discount unconditional discount in BRL, from 0 up to {@code amount}
 * @param deduction ISS base deduction declared by the issuer, in BRL: greater than 0 and less than
 *     {@code amount} minus {@code discount} (0 is the same as omitting it). The invoice is still
 *     issued for the full {@code amount}; the issuer is responsible for the declared value.
 * @param email request the email notification (default false); {@code options.email.send} wins
 *     when set
 */
public record InvoiceCreateParams(
		String companyId,
		String companyTaxId,
		String externalId,
		Customer customer,
		Service service,
		Double amount,
		String competenceDate,
		Double discount,
		Double deduction,
		Boolean email,
		Options options) {

	/** Without {@code deduction}; kept so existing callers of the previous constructor still compile. */
	public InvoiceCreateParams(
			String companyId,
			String companyTaxId,
			String externalId,
			Customer customer,
			Service service,
			Double amount,
			String competenceDate,
			Double discount,
			Boolean email,
			Options options) {
		this(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount,
				null, email, options);
	}

	/** A service-only invoice: the code and the city default to the company's. */
	public static InvoiceCreateParams of(
			Customer customer, Service service, double amount, String competenceDate) {
		return new InvoiceCreateParams(
				null, null, null, customer, service, amount, competenceDate, null, null, null, null);
	}

	public InvoiceCreateParams withCompanyId(String companyId) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount, deduction,
				email, options);
	}

	public InvoiceCreateParams withCompanyTaxId(String companyTaxId) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount, deduction,
				email, options);
	}

	public InvoiceCreateParams withExternalId(String externalId) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount, deduction,
				email, options);
	}

	public InvoiceCreateParams withDiscount(double discount) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount, deduction,
				email, options);
	}

	public InvoiceCreateParams withDeduction(double deduction) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount, deduction,
				email, options);
	}

	public InvoiceCreateParams withOptions(Options options) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount, deduction,
				email, options);
	}

	/** When a customer is sent, {@code taxId} (CPF or CNPJ) is required; the rest is optional. */
	public record Customer(String taxId, String name, String email, CustomerAddress address) {

		public static Customer ofTaxId(String taxId) {
			return new Customer(taxId, null, null, null);
		}

		public static Customer of(String name, String taxId) {
			return new Customer(taxId, name, null, null);
		}

		public Customer withEmail(String email) {
			return new Customer(taxId, name, email, address);
		}
	}

	public record CustomerAddress(
			String street, String city, String state, String zip, String country) {}

	/**
	 * Every field is optional: {@code code} (national service code, 6 digits) defaults to the
	 * company's and then to {@code 100901}, {@code municipalityCode} (IBGE, 7 digits) to the
	 * company's city, and {@code description} to the name of the service code.
	 */
	public record Service(String code, String municipalityCode, String description) {

		public static Service of(String code, String municipalityCode) {
			return new Service(code, municipalityCode, null);
		}

		public static Service ofDescription(String description) {
			return new Service(null, null, description);
		}

		public Service withDescription(String description) {
			return new Service(code, municipalityCode, description);
		}
	}

	/** {@code type} is {@code IMMEDIATE} (default) or {@code SCHEDULE} with a future competence date. */
	public record Options(Email email, String type) {

		public static Options sendEmail(boolean send) {
			return new Options(new Email(send), null);
		}

		public static Options schedule() {
			return new Options(null, "SCHEDULE");
		}
	}

	public record Email(Boolean send) {}
}
