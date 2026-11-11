package com.kodegravity.exr.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every inbound request a correlation id, publishes it to the logging
 * context (MDC) and echoes it back on the response.
 *
 * <p>Why this exists: one user action touches several services. Without a shared
 * id, the logs of that single action are impossible to find among thousands of
 * unrelated lines.
 *
 * <p>This is deliberately NOT a request-id generator that trusts the caller: the
 * incoming header is treated as untrusted input (see {@link #sanitise(String)}).
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** The header name used on the wire, in both directions. */
    public static final String HEADER = "X-Correlation-Id";

    /** The key the logging framework reads, e.g. logback pattern %X{correlationId}. */
    public static final String MDC_KEY = "correlationId";

    /** Longer values are refused: an id is a tag, not a payload. */
    private static final int MAX_LENGTH = 64;

    /**
     * Whitelist. Anything outside this set is rejected, which also blocks
     * CRLF injection - otherwise a caller could send "abc\r\nINFO fake log line"
     * and forge entries in our logs.
     */
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]+");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        String correlationId = sanitise(request.getHeader(HEADER));
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        // Echo it back so a user (or a support ticket) can quote the exact id.
        response.setHeader(HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Threads are reused to serve other requests. Leaving the value behind
            // would stamp THIS request's id onto an unrelated request's log lines.
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * Accepts the caller's value only if it is short and contains safe characters.
     *
     * @return the value to use, or {@code null} if it must be replaced
     */
    static String sanitise(String candidate) {
        if (candidate == null) {
            return null;
        }
        String trimmed = candidate.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            return null;
        }
        return SAFE.matcher(trimmed).matches() ? trimmed : null;
    }
}
