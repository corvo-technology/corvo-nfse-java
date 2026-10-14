package technology.corvo.nfse.types;

/**
 * Input for {@code invoices.create}. Only the customer, service, amount, and competenceDate are
 * required.
 */
public record InvoiceCreateParams(
		String companyId,
		String companyTaxId,
		String externalId,
		String correlationId,
		Customer customer,
		Service service,
		Double amount,
		String competenceDate,
		String email,
		Options options) {

	public static InvoiceCreateParams of(
			Customer customer, Service service, double amount, String competenceDate) {
		return new InvoiceCreateParams(null, null, null, null, customer, service, amount, competenceDate, null, null);
	}

	public InvoiceCreateParams withCompanyId(String companyId) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, correlationId, customer, service, amount, competenceDate, email,
				options);
	}

	public InvoiceCreateParams withExternalId(String externalId) {
		return new InvoiceCreateParams(
				companyId, companyTaxId, externalId, correlationId, customer, service, amount, competenceDate, email,
				options);
	}

	public record Customer(String name, String taxId, String email, CustomerAddress address) {

		public static Customer of(String name, String taxId) {
			return new Customer(name, taxId, null, null);
		}

		public Customer withEmail(String email) {
			return new Customer(name, taxId, email, address);
		}
	}

	public record CustomerAddress(
			String street, String city, String state, String zip, String country) {}

	public record Service(String code, String municipalityCode, String description) {

		public static Service of(String code, String municipalityCode) {
			return new Service(code, municipalityCode, null);
		}

		public Service withDescription(String description) {
			return new Service(code, municipalityCode, description);
		}
	}

	public record Options(Email email) {

		public static Options sendEmail(boolean send) {
			return new Options(new Email(send));
		}
	}

	public record Email(Boolean send) {}
}
