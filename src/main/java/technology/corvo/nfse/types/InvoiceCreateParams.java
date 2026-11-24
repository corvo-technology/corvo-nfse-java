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
 * @param purchaseOrder purchase or service order number the customer wants on the invoice (up to 60
 *     characters; DPS {@code xPed}). Changes no tax.
 * @param notes free text printed on the invoice (up to 2000 characters; DPS {@code xInfComp}).
 *     Changes no tax.
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
		String purchaseOrder,
		String notes,
		Boolean email,
		Options options) {

	/** Without {@code purchaseOrder} and {@code notes}; kept so callers of the 0.2.0 constructor still compile. */
	public InvoiceCreateParams(
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
		this(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount,
				deduction, null, null, email, options);
	}

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
				null, null, null, email, options);
	}

	/** A service-only invoice: the code and the city default to the company's. */
	public static InvoiceCreateParams of(
			Customer customer, Service service, double amount, String competenceDate) {
		return new InvoiceCreateParams(
				null, null, null, customer, service, amount, competenceDate, null, null, null, null, null, null);
	}

	public InvoiceCreateParams withCompanyId(String companyId) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount, deduction,
				purchaseOrder, notes, email, options);
	}

	public InvoiceCreateParams withCompanyTaxId(String companyTaxId) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount, deduction,
				purchaseOrder, notes, email, options);
	}

	public InvoiceCreateParams withExternalId(String externalId) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount, deduction,
				purchaseOrder, notes, email, options);
	}

	public InvoiceCreateParams withDiscount(double discount) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount, deduction,
				purchaseOrder, notes, email, options);
	}

	public InvoiceCreateParams withDeduction(double deduction) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount, deduction,
				purchaseOrder, notes, email, options);
	}

	public InvoiceCreateParams withPurchaseOrder(String purchaseOrder) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount, deduction,
				purchaseOrder, notes, email, options);
	}

	public InvoiceCreateParams withNotes(String notes) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount, deduction,
				purchaseOrder, notes, email, options);
	}

	public InvoiceCreateParams withOptions(Options options) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, customer, service, amount, competenceDate, discount, deduction,
				purchaseOrder, notes, email, options);
	}

	/**
	 * A Brazilian customer ({@code taxId}, CPF or CNPJ; {@code name} may be omitted when the customer
	 * was already saved) or a {@code foreign} one (then {@code taxId} and {@code address} must be
	 * {@code null} and {@code name} is required).
	 */
	public record Customer(
			String taxId, String name, String email, CustomerAddress address, ForeignCustomer foreign) {

		/** Without {@code foreign}; kept so callers of the 0.2.0 constructor still compile. */
		public Customer(String taxId, String name, String email, CustomerAddress address) {
			this(taxId, name, email, address, null);
		}

		/** A customer outside Brazil. */
		public static Customer ofForeign(String name, ForeignCustomer foreign) {
			return new Customer(null, name, null, null, foreign);
		}

		public static Customer ofTaxId(String taxId) {
			return new Customer(taxId, null, null, null, null);
		}

		public static Customer of(String name, String taxId) {
			return new Customer(taxId, name, null, null, null);
		}

		public Customer withEmail(String email) {
			return new Customer(taxId, name, email, address, foreign);
		}
	}

	public record CustomerAddress(
			String street, String city, String state, String zip, String country, String municipalityCode) {

		/** Without {@code municipalityCode}; kept so callers of the 0.2.0 constructor still compile. */
		public CustomerAddress(String street, String city, String state, String zip, String country) {
			this(street, city, state, zip, country, null);
		}
	}

	/**
	 * Every field is optional: {@code code} (national service code, 6 digits) defaults to the
	 * company's and then to {@code 100901}, {@code municipalityCode} (IBGE, 7 digits) to the
	 * company's city, and {@code description} to the name of the service code.
	 */
	public record Service(
			String code,
			String municipalityCode,
			String description,
			String serviceProfileId,
			ServiceFiscal fiscal) {

		/** Without {@code serviceProfileId} and {@code fiscal}; kept so callers of the 0.2.0 constructor still compile. */
		public Service(String code, String municipalityCode, String description) {
			this(code, municipalityCode, description, null, null);
		}

		public static Service of(String code, String municipalityCode) {
			return new Service(code, municipalityCode, null);
		}

		public static Service ofDescription(String description) {
			return new Service(null, null, description);
		}

		public Service withDescription(String description) {
			return new Service(code, municipalityCode, description, serviceProfileId, fiscal);
		}
	}

	/**
	 * {@code type} is {@code IMMEDIATE} (default), {@code SCHEDULE} or {@code RECURRING}. The last two
	 * create a schedule instead of issuing now ({@code competenceDate} is the date of the first run,
	 * from tomorrow on); {@code recurrence} is required for {@code RECURRING} and refused otherwise.
	 */
	public record Options(Email email, String type, RecurrenceOptions recurrence) {

		/** Without {@code recurrence}; kept so callers of the 0.2.0 constructor still compile. */
		public Options(Email email, String type) {
			this(email, type, null);
		}

		/** A monthly, biweekly or weekly series: {@code frequency} is {@code WEEKLY}, {@code BIWEEKLY} or {@code MONTHLY}. */
		public static Options recurring(String frequency, String endsOn) {
			return new Options(null, "RECURRING", new RecurrenceOptions(endsOn, frequency));
		}

		public static Options sendEmail(boolean send) {
			return new Options(new Email(send), null, null);
		}

		public static Options schedule() {
			return new Options(null, "SCHEDULE", null);
		}
	}

	public record Email(Boolean send) {}
}
