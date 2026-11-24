package technology.corvo.nfse.resources;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import technology.corvo.nfse.transport.HttpMethod;
import technology.corvo.nfse.transport.HttpTransport;
import technology.corvo.nfse.transport.Result;
import technology.corvo.nfse.transport.UnwrapMode;
import technology.corvo.nfse.types.Paginated;
import technology.corvo.nfse.types.Schedule;
import technology.corvo.nfse.types.ScheduleRun;
import technology.corvo.nfse.types.UpdateSchedule;

/**
 * Scheduled and recurring invoices. A schedule is created with {@code invoices().create(...)} and
 * {@code options.type} {@code SCHEDULE} or {@code RECURRING}. Fields are {@code snake_case}.
 */
public final class SchedulesResource {

	private static final String BASE = "/schedules";

	private final HttpTransport http;

	public SchedulesResource(HttpTransport http) {
		this.http = http;
	}

	/** Lists one page of schedules. The cursor is the offset of the next page: pass it back untouched. */
	public CompletableFuture<Result<Paginated<Schedule>>> list(
			String status, String companyId, Integer limit, String cursor) {
		return http.list(BASE, Query.of("status", status, "companyId", companyId, "limit", limit, "cursor", cursor),
				UnwrapMode.NONE, Schedule.class);
	}

	/**
	 * Walks every page of schedules lazily.
	 *
	 * <p><b>Documented exception to the never-throw rule:</b> if fetching a page fails, a
	 * {@code CorvoApiError} is thrown from the stream.
	 */
	public Stream<Schedule> iterate(String status, String companyId, Integer limit, String cursor) {
		return http.iterate(BASE, Query.of("status", status, "companyId", companyId, "limit", limit, "cursor", cursor),
				UnwrapMode.NONE, Schedule.class);
	}

	/** Fetches a schedule. */
	public CompletableFuture<Result<Schedule>> get(String id) {
		return http.request(HttpMethod.GET, path(id), null, null, null, UnwrapMode.DATA, Schedule.class);
	}

	/** Changes a schedule; applies to the next runs only. {@code null} fields are not sent. */
	public CompletableFuture<Result<Schedule>> update(String id, UpdateSchedule params) {
		return http.request(HttpMethod.PATCH, path(id), params, null, null, UnwrapMode.DATA, Schedule.class);
	}

	/** Same with a raw patch that keeps {@code null} values ({@code "ends_on": null} removes the end date). */
	public CompletableFuture<Result<Schedule>> update(String id, Map<String, Object> patch) {
		return http.request(HttpMethod.PATCH, path(id), patch, null, null, UnwrapMode.DATA, Schedule.class);
	}

	/** Pauses a schedule. */
	public CompletableFuture<Result<Schedule>> pause(String id) {
		return action(id, "pause");
	}

	/** Resumes a paused schedule at its next occurrence. */
	public CompletableFuture<Result<Schedule>> resume(String id) {
		return action(id, "resume");
	}

	/** Cancels a schedule (final). */
	public CompletableFuture<Result<Schedule>> cancel(String id) {
		return action(id, "cancel");
	}

	/** Lists one page of the attempts to run a schedule, newest first. */
	public CompletableFuture<Result<Paginated<ScheduleRun>>> listRuns(String id, Integer limit, String cursor) {
		return http.list(path(id) + "/runs", Query.of("limit", limit, "cursor", cursor), UnwrapMode.NONE,
				ScheduleRun.class);
	}

	/**
	 * Walks every run of a schedule lazily.
	 *
	 * <p><b>Documented exception to the never-throw rule:</b> a page failure throws {@code CorvoApiError}.
	 */
	public Stream<ScheduleRun> iterateRuns(String id, Integer limit, String cursor) {
		return http.iterate(path(id) + "/runs", Query.of("limit", limit, "cursor", cursor), UnwrapMode.NONE,
				ScheduleRun.class);
	}

	private CompletableFuture<Result<Schedule>> action(String id, String action) {
		return http.request(HttpMethod.POST, path(id) + "/" + action, null, null, null, UnwrapMode.DATA,
				Schedule.class);
	}

	private static String path(String id) {
		return BASE + "/" + CompaniesResource.encode(id);
	}
}
