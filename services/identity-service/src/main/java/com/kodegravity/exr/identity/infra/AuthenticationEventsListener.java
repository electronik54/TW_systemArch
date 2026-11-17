package com.kodegravity.exr.identity.infra;

import com.kodegravity.exr.identity.application.LoginAttemptService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Turns Spring Security's announcements into our own counter.
 *
 * <p>Why an event listener instead of a check inside the filter: the framework
 * already knows when an authentication failed and why. Listening keeps the policy
 * out of the security wiring, and it means the same counting applies to any
 * authentication method added later - Basic today, a login endpoint or a token
 * grant tomorrow.
 *
 * <p><b>Which failures are counted, and which are not.</b> Only
 * {@link AuthenticationFailureBadCredentialsEvent} - a wrong password or an
 * unknown email. A locked or disabled account produces a different event and is
 * deliberately NOT counted: punishing an already-closed door would extend the
 * lock on every attempt, and a stranger could keep an account shut for ever by
 * knocking.
 *
 * <p><b>Why {@code getName()} is safe here.</b> For Basic authentication it is the
 * username exactly as typed. When the email is unknown, the service below finds no
 * row and does nothing - the count stays zero, because there is nothing to count
 * on. The client learns nothing either way.
 */
@Component
public class AuthenticationEventsListener {

    private final LoginAttemptService loginAttempts;

    public AuthenticationEventsListener(LoginAttemptService loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    @EventListener
    public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        loginAttempts.recordFailure(event.getAuthentication().getName());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        loginAttempts.recordSuccess(event.getAuthentication().getName());
    }
}
