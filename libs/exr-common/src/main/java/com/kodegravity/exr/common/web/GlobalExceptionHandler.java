package com.kodegravity.exr.common.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

/**
 * Turns exceptions into consistent RFC 7807 responses.
 *
 * <p>Controllers therefore never build error payloads themselves - one shape,
 * one place to change it.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Bean validation failures: 400 with a per-field list the UI can highlight. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onValidationFailure(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> Map.of(
                        "field", fieldError.getField(),
                        "message", fieldError.getDefaultMessage() == null
                                ? "is invalid"
                                : fieldError.getDefaultMessage()))
                .toList();

        ProblemDetail problem = ProblemSupport.create(HttpStatus.BAD_REQUEST,
                "validation-failed",
                "Request validation failed",
                "One or more fields are invalid.");
        problem.setProperty("errors", errors);
        return problem;
    }

    /** Unparseable JSON. The parser message mentions internal class names, so keep it out. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail onUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("Unreadable request body", ex);
        return ProblemSupport.create(HttpStatus.BAD_REQUEST,
                "malformed-json",
                "Malformed request body",
                "The request body could not be parsed as JSON.");
    }

    /** Unknown path. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail onNoResource(NoResourceFoundException ex) {
        log.debug("No resource for {}", ex.getResourcePath());
        return ProblemSupport.create(HttpStatus.NOT_FOUND,
                "not-found",
                "Not found",
                "The requested resource does not exist.");
    }

    /**
     * Catch-all. Two things matter here:
     * <ol>
     *   <li>If Spring already produced a correct status (405, 415, 404...), keep
     *       it - turning a client mistake into a 500 creates phantom outages.</li>
     *   <li>Never return the exception message or stack trace to the caller. Log
     *       it server-side, where the correlation id ties it to the request.</li>
     * </ol>
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail onUnexpected(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            return ProblemSupport.decorate(errorResponse.getBody());
        }
        log.error("Unhandled exception", ex);
        return ProblemSupport.create(HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error",
                "Unexpected error",
                "An unexpected error occurred. Quote the correlation id when reporting this.");
    }
}
