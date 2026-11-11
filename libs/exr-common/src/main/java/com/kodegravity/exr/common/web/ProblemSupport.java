package com.kodegravity.exr.common.web;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

/**
 * Builders for RFC 7807 "problem detail" responses.
 *
 * <p>Every service returns errors in this shape, so clients parse one format
 * instead of five. Each response also carries the correlation id as a custom
 * extension member (RFC 7807 section 3.2), which is what lets a user paste an
 * error into a support ticket and have us find the matching log lines.
 */
public final class ProblemSupport {

    /** Custom extension member holding the request's correlation id. */
    public static final String CORRELATION_ID = "correlationId";

    /**
     * Namespace for the "type" member. In a real product this points at a docs
     * page explaining the error, so a client developer can look up what it means.
     */
    private static final String TYPE_NAMESPACE = "https://errors.exr.local/";

    private ProblemSupport() {
        // static helpers only
    }

    /**
     * @param status HTTP status to send
     * @param code   short machine-readable code, also used inside the "type" URI
     * @param title  short human-readable summary (constant per code)
     * @param detail explanation of this specific occurrence
     */
    public static ProblemDetail create(HttpStatus status, String code, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(TYPE_NAMESPACE + code));
        problem.setTitle(title);
        problem.setDetail(detail);
        return decorate(problem);
    }

    /**
     * Adds the correlation id to an existing problem (used when Spring has already
     * built a correct one, e.g. for 405 or 415).
     */
    public static ProblemDetail decorate(ProblemDetail problem) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            problem.setProperty(CORRELATION_ID, correlationId);
        }
        return problem;
    }
}
