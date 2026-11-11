package com.kodegravity.exr.common.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behaviour tests for {@link CorrelationIdFilter}.
 *
 * <p>These test what an HTTP client can observe plus what the logger would see -
 * not private internals. Each case exists because it guards a specific failure.
 */
class CorrelationIdFilterTest {

    private static final String HEADER = CorrelationIdFilter.HEADER;
    private static final String MDC_KEY = CorrelationIdFilter.MDC_KEY;

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        // Test threads are reused too - do not leak state between tests.
        MDC.clear();
    }

    @Test
    @DisplayName("no incoming header -> a fresh id is generated and returned")
    void generatesIdWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/events");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HEADER)).isNotBlank();
    }

    @Test
    @DisplayName("valid incoming header -> reused, not replaced")
    void reusesValidIncomingId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/events");
        request.addHeader(HEADER, "abc-123");

        // Capture what the logger would have seen DURING the request.
        AtomicReference<String> seenByChain = new AtomicReference<>();
        FilterChain captureChain = (req, res) -> seenByChain.set(MDC.get(MDC_KEY));

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, captureChain);

        assertThat(seenByChain.get()).isEqualTo("abc-123");
        assertThat(response.getHeader(HEADER)).isEqualTo("abc-123");
    }

    @Test
    @DisplayName("header with CRLF (log injection) -> rejected and replaced")
    void replacesUnsafeIncomingId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/events");
        // Without sanitising, this would forge a fake log line.
        request.addHeader(HEADER, "abc\r\nINFO Login succeeded for admin");

        AtomicReference<String> seenByChain = new AtomicReference<>();
        FilterChain captureChain = (req, res) -> seenByChain.set(MDC.get(MDC_KEY));

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, captureChain);

        assertThat(seenByChain.get())
                .isNotEqualTo("abc\r\nINFO Login succeeded for admin")
                .doesNotContain("\r")
                .doesNotContain("\n");
    }

    @Test
    @DisplayName("over-long header -> rejected and replaced")
    void replacesOverlongId() {
        assertThat(CorrelationIdFilter.sanitise("a".repeat(65))).isNull();
        assertThat(CorrelationIdFilter.sanitise("a".repeat(64))).isNotNull();
    }

    @Test
    @DisplayName("after the request the id is removed from the MDC")
    void clearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/events");
        request.addHeader(HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        // A leftover value here would stamp this id onto the NEXT request's logs.
        assertThat(MDC.get(MDC_KEY)).isNull();
    }
}
