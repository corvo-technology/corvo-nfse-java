package technology.corvo.nfse.types;

import java.util.List;

/**
 * A page of results: the items plus pagination metadata.
 *
 * @param <T> item type
 */
public record Paginated<T>(List<T> data, Pagination pagination) {}
