package com.kodegravity.exr.identity.api;

import com.kodegravity.exr.common.web.ProblemSupport;
import com.kodegravity.exr.identity.domain.EmailAlreadyUsedException;
import com.kodegravity.exr.identity.domain.WeakPasswordException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns a business refusal into an HTTP answer a client can act on.
 *
 * <p>Why this exists, measured: without it, registering a taken email produced
 * {@code 500 Unexpected error} — the correct outcome (no duplicate row, thanks to
 * the unique index) with the wrong explanation. A 500 tells an integration "the
 * server is broken, retry or page someone"; {@code 409 Conflict} tells it "the
 * request was understood and refused on its merits", and the body names the field
 * so a form can highlight it.
 *
 * <p>Why the handler is per service and not in {@code exr-common}: a shared handler
 * would have to know every domain exception in the platform, which is exactly the
 * coupling the common library must avoid. It shares the <em>shape</em>
 * ({@link ProblemSupport}) and nothing else.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * 409 is the honest status for "this state already exists". 400 would blame
     * the request's syntax, which is fine, and 422 is used by some APIs for the
     * same idea — 409 is the one that says "the conflict is with data, not
     * grammar".
     */
    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ProblemDetail emailAlreadyUsed(EmailAlreadyUsedException exception) {
        return ProblemSupport.create(
                HttpStatus.CONFLICT,
                "email-already-used",
                "Email already registered",
                exception.getMessage());
    }

    /**
     * 400, because nothing is in conflict and nothing exists yet: the request
     * itself is not acceptable.
     *
     * <p>The violations ride along as an extension member so a form can mark the
     * password field, while {@code detail} stays a single readable sentence for
     * anybody reading a log.
     */
    @ExceptionHandler(WeakPasswordException.class)
    public ProblemDetail weakPassword(WeakPasswordException exception) {
        ProblemDetail problem = ProblemSupport.create(
                HttpStatus.BAD_REQUEST,
                "weak-password",
                "Password does not meet the policy",
                exception.getMessage());
        problem.setProperty("violations", exception.getViolations());
        return problem;
    }

    /**
     * 401 for every way a login can fail: wrong password, unknown email, locked
     * account, suspended account.
     *
     * <p><b>One status and one sentence for four causes, on purpose.</b> A message that
     * distinguishes "no such email" from "wrong password" turns the endpoint into an
     * account-enumeration oracle: an attacker learns which addresses are registered
     * without guessing a single password. The detailed reason is already in the
     * database ({@code failed_attempts}, {@code locked_until}) and in the audit trail,
     * which is where an operator should look.
     *
     * <p>{@code AuthenticationException} is the Spring parent of
     * {@code BadCredentialsException}, {@code LockedException} and
     * {@code DisabledException}, so one handler covers the three refusals our
     * {@code DbUserDetailsService} can produce.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail couldNotSignIn(AuthenticationException exception) {
        return ProblemSupport.create(
                HttpStatus.UNAUTHORIZED,
                "invalid-credentials",
                "Could not sign in",
                "The email and password did not match an active account.");
    }
}
