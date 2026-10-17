package technology.corvo.nfse.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import technology.corvo.nfse.TestHarness;
import technology.corvo.nfse.error.CorvoApiError;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.types.Company;
import technology.corvo.nfse.types.CompanyCreateParams;
import technology.corvo.nfse.types.CompanyUpdateParams;
import technology.corvo.nfse.types.Paginated;

class CompaniesResourceTest extends TestHarness {

	private static final String COMPANY_JSON =
			"{\"id\":\"comp_1\",\"tenantId\":\"t_1\",\"legalName\":\"Acme Ltda\",\"tradeName\":null,"
					+ "\"taxId\":\"52998224725\",\"municipalReg\":null,\"municipalityCode\":\"3170206\","
					+ "\"taxRegime\":\"Simples Nacional\",\"simpleNational\":true,\"specialRegime\":null,"
					+ "\"addressStreet\":\"Rua A\",\"addressNumber\":\"1\",\"addressComplement\":null,"
					+ "\"addressDistrict\":\"Centro\",\"addressCity\":\"Belo Horizonte\",\"addressState\":\"MG\","
					+ "\"addressZip\":\"30000-000\",\"fiscalSchemaVersion\":\"1.00\",\"environment\":\"test\","
					+ "\"status\":\"active\",\"createdAt\":\"2026-10-01T00:00:00Z\","
					+ "\"updatedAt\":\"2026-10-01T00:00:00Z\",\"deletedAt\":null,\"setupGaps\":[]}";

	@Test
	void createPostsToCompaniesPath() throws Exception {
		enqueueJson(200, "{\"data\":" + COMPANY_JSON + "}");

		Result<Company> result =
				corvo().companies()
						.create(CompanyCreateParams.of("Acme Ltda", "52998224725", "Simples Nacional", "3550308"))
						.get(5, TimeUnit.SECONDS);

		assertThat(((Result.Ok<Company>) result).data().legalName()).isEqualTo("Acme Ltda");
		RecordedRequest request = takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).isEqualTo("/v1/companies");
		assertThat(request.getBody().readUtf8()).contains("\"taxId\":\"52998224725\"");
	}

	@Test
	void getUpdateRemoveUseIdInPath() throws Exception {
		enqueueJson(200, "{\"data\":" + COMPANY_JSON + "}");
		enqueueJson(200, "{\"data\":" + COMPANY_JSON + "}");
		enqueueJson(204, "");

		corvo().companies().get("comp_1").get(5, TimeUnit.SECONDS);
		corvo().companies().update("comp_1", new CompanyUpdateParams(null, "New Name", null, null, null, null, null,
						null, null, null))
				.get(5, TimeUnit.SECONDS);
		Result<Void> removed =
				corvo().companies().remove("comp_1").get(5, TimeUnit.SECONDS);

		assertThat(takeRequest().getPath()).isEqualTo("/v1/companies/comp_1");
		RecordedRequest patch = takeRequest();
		assertThat(patch.getMethod()).isEqualTo("PATCH");
		assertThat(patch.getBody().readUtf8()).isEqualTo("{\"tradeName\":\"New Name\"}");
		RecordedRequest delete = takeRequest();
		assertThat(delete.getMethod()).isEqualTo("DELETE");
		// 204 -> null payload
		assertThat(((Result.Ok<Void>) removed).data()).isNull();
	}

	@Test
	void listDecodesPaginatedCompanies() throws Exception {
		enqueueJson(
				200,
				"{\"data\":[" + COMPANY_JSON + "],\"pagination\":{\"limit\":10,\"hasMore\":false,"
						+ "\"nextCursor\":null}}");

		Result<Paginated<Company>> result =
				corvo().companies().list(10, null).get(5, TimeUnit.SECONDS);

		Paginated<Company> page = ((Result.Ok<Paginated<Company>>) result).data();
		assertThat(page.data()).hasSize(1);
		assertThat(page.data().get(0).setupGaps()).isEmpty();
		assertThat(takeRequest().getRequestUrl().queryParameter("limit")).isEqualTo("10");
	}

	@Test
	void iterateWalksAllPages() throws Exception {
		enqueueJson(
				200,
				"{\"data\":[" + COMPANY_JSON + "],\"pagination\":{\"limit\":1,\"hasMore\":true,"
						+ "\"nextCursor\":\"cur_2\"}}");
		enqueueJson(
				200,
				"{\"data\":[" + COMPANY_JSON.replace("comp_1", "comp_2") + "],"
						+ "\"pagination\":{\"limit\":1,\"hasMore\":false,\"nextCursor\":null}}");

		List<Company> all = corvo().companies().iterate(1, null).toList();

		assertThat(all).extracting(Company::id).containsExactly("comp_1", "comp_2");
		takeRequest();
		assertThat(takeRequest().getRequestUrl().queryParameter("cursor")).isEqualTo("cur_2");
	}

	@Test
	void iterateThrowsTypedErrorOnFirstPageFailure() throws Exception {
		enqueueJson(401, "{\"code\":\"ERROR_UNAUTHORIZED\",\"message\":\"bad key\"}");

		assertThatThrownBy(() -> corvo().companies().iterate(10, null).toList())
				.isInstanceOf(CorvoApiError.class)
				.extracting(t -> ((CorvoApiError) t).code())
				.isEqualTo("ERROR_UNAUTHORIZED");
	}

	private technology.corvo.nfse.Corvo corvo() {
		return technology.corvo.nfse.Corvo.builder()
				.apiKey(API_KEY)
				.baseUrl(server.url("/").toString())
				.build();
	}
}
