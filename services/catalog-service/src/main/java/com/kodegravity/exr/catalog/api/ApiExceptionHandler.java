package com.kodegravity.exr.catalog.api;

import com.kodegravity.exr.common.web.ProblemSupport;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Day 5.4: makes a method-security refusal answer <b>403</b> instead of <b>500</b>.
 *
 * <p><b>The bug this class fixes, and why it happens at all.</b> {@code @PreAuthorize}
 * throws {@link AccessDeniedException} <em>inside</em> the controller call. That
 * exception travels back through {@code DispatcherServlet}, where
 * {@code exr-common}'s {@code GlobalExceptionHandler} — which handles
 * {@code Exception} — catches it first and honestly reports "unexpected error: 500".
 * Spring Security's {@code ExceptionTranslationFilter}, the component that knows a
 * denial must be a {@code 403}, sits <b>outside</b> the servlet and never sees the
 * exception, so its knowledge is bypassed.
 *
 * <p><b>Two details make this advice win:</b> {@code @Order(HIGHEST_PRECEDENCE)} puts
 * it ahead of the shared advice, and the handler is more specific than
 * {@code Exception}, which is what lets resolution pick it inside this advice.
 *
 * <p><b>Why a per-service advice rather than a change in {@code exr-common}.</b>
 * {@code exr-common} holds web primitives and deliberately knows nothing about Spring
 * Security; giving it a security exception type would put a security dependency into
 * the library every service inherits. Each service that uses method security answers
 * for its own refusals - which is also where the wording of that answer belongs.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    /**
     * 403, not 401: the caller was identified by a valid token, and the refusal is
     * about permission. Retrying with the same token changes nothing, and a 401 would
     * send a client into a pointless login loop.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail forbidden(AccessDeniedException exception) {
        return ProblemSupport.create(
                HttpStatus.FORBIDDEN,
                "forbidden",
                "Not permitted",
                "Your role does not allow this operation.");
    }
}
