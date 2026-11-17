package com.kodegravity.exr.identity.domain;

import java.util.List;

/**
 * A password the {@link PasswordPolicy} refused.
 *
 * <p>Carries the list of violations rather than a formatted string, so the layer
 * above decides how to present them: one sentence in an API body, a bullet list in
 * a form, a single toast in a mobile app. Formatting here would hard-code one
 * client's idea of a good message into the domain.
 *
 * <p>The rejected password is deliberately **not** stored in the exception. It
 * would then appear in stack traces and logs, which is exactly where a rejected
 * password must never be - most users mistype a real one before inventing a new
 * one.
 */
public class WeakPasswordException extends RuntimeException {

    private final List<String> violations;

    public WeakPasswordException(List<String> violations) {
        super("Password rejected: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
