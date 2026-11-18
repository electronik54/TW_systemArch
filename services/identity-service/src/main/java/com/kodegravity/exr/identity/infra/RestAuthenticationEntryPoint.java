package com.kodegravity.exr.identity.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodegravity.exr.common.web.ProblemSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * What a client sees when it is refused: the same RFC 7807 body as every other
 * error, with the same correlation id.
 *
 * <p><b>The gap this closes, measured before it was written.</b> Without it, an
 * unauthenticated request to a protected endpoint produced
 * {@code 401} with {@code Content-Length: 0}, no correlation id, and no body —
 * Spring Security's own default. A client could not tell a refusal from a network
 * cut, and support could not find the request in the logs. That is the worst
 * possible answer for the one case somebody will actually complain about.
 *
 * <p><b>Why the correlation id appears here at all</b> depends on the other half
 * of the fix: {@code CorrelationIdFilter} now runs before the security chain, so
 * the id is already in the logging context by the time this class writes its
 * body. Order without the entry point gives an id on an empty body; the entry
 * point without the order gives a body with no id.
 *
 * <p>{@code WWW-Authenticate} is required by RFC 7235 on a 401 — it is what tells
 * the client which scheme to use. Removing it makes some clients retry blindly.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        ProblemDetail problem = ProblemSupport.create(
                HttpStatus.UNAUTHORIZED,
                "unauthorized",
                "Unauthorized",
                "This resource requires authentication. Send valid credentials.");

        // Order matters: status and headers first, body last. Padding and
        // content-length are decided when the stream is written.
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"exr\"");

        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
