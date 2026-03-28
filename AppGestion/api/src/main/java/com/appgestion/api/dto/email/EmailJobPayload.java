package com.appgestion.api.dto.email;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * JSON almacenado en {@code email_jobs.payload_json}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmailJobPayload(
        String kind,
        String to,
        String subject,
        String htmlBody,
        String replyTo,
        String pdfBase64,
        String pdfFilename,
        List<SupportAttachmentPayload> attachments
) {
    public static final String KIND_HTML_PDF = "HTML_PDF";
    public static final String KIND_HTML_CLIENT = "HTML_CLIENT";
    public static final String KIND_SUPPORT = "SUPPORT";

    public record SupportAttachmentPayload(String filename, String contentBase64, String contentType) {}
}
