package technology.corvo.nfse.types;

/**
 * Cursor pagination metadata returned with every list response.
 *
 * @param limit page size (1–1000, default 50)
 * @param hasMore whether more pages exist
 * @param nextCursor opaque base64url cursor; {@code null} when {@code hasMore} is false
 */
public record Pagination(int limit, boolean hasMore, String nextCursor) {}
