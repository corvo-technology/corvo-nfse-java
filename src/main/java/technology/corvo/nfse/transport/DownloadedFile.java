package technology.corvo.nfse.transport;

/**
 * A binary download (invoice PDF/XML): the raw bytes plus the server-provided
 * {@code Content-Type} and {@code Content-Disposition} filename, when present.
 *
 * @param content raw file bytes
 * @param mimeType value of the {@code Content-Type} header, or {@code null}
 * @param fileName filename parsed from {@code Content-Disposition}, or {@code null}
 */
public record DownloadedFile(byte[] content, String mimeType, String fileName) {}
