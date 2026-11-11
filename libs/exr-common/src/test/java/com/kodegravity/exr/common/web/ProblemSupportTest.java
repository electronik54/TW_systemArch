package com.kodegravity.exr.common.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemSupportTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("produces the documented RFC 7807 members")
    void buildsTheStandardMembers() {
        ProblemDetail problem = ProblemSupport.create(HttpStatus.BAD_REQUEST,
                "validation-failed", "Request validation failed", "One or more fields are invalid.");

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Request validation failed");
        assertThat(problem.getDetail()).isEqualTo("One or more fields are invalid.");
        assertThat(problem.getType()).hasToString("https://errors.exr.local/validation-failed");
    }

    @Test
    @DisplayName("attaches the correlation id when one is in scope")
    void attachesCorrelationId() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "abc-123");

        ProblemDetail problem = ProblemSupport.create(HttpStatus.NOT_FOUND,
                "not-found", "Not found", "The requested resource does not exist.");

        assertThat(problem.getProperties())
                .containsEntry(ProblemSupport.CORRELATION_ID, "abc-123");
    }

    @Test
    @DisplayName("stays valid when there is no correlation id (e.g. a background job)")
    void omitsCorrelationIdWhenAbsent() {
        ProblemDetail problem = ProblemSupport.create(HttpStatus.NOT_FOUND,
                "not-found", "Not found", "The requested resource does not exist.");

        assertThat(problem.getProperties()).isNullOrEmpty();
    }
}
