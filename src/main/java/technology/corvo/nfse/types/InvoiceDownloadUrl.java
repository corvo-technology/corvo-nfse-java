package technology.corvo.nfse.types;

/** A presigned URL for downloading an invoice document. */
public record InvoiceDownloadUrl(String downloadUrl, String expiresAt, String fileName, String mimeType) {}
