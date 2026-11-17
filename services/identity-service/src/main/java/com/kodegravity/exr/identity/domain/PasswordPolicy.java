package com.kodegravity.exr.identity.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What counts as an acceptable password. A business rule, so it lives in
 * {@code domain}: it knows nothing about HTTP, JSON or Spring.
 *
 * <p><b>Why not the classic "one uppercase, one digit, one symbol" rule.</b> That
 * rule has been studied to death, and its effect is predictable: it pushes people
 * to {@code Password1!}, which satisfies every check and is cracked in seconds,
 * while a long passphrase like {@code correct horse battery staple} is rejected
 * for having no symbol. The modern guidance (NIST SP 800-63B) is the opposite:
 * <b>length is the lever, composition is theatre</b>, and a blocklist of known
 * passwords catches the ones people actually pick.
 *
 * <p>So this policy has exactly three rules, and each can be explained to a user
 * in one sentence:
 * <ol>
 *   <li>at least {@link #MIN_LENGTH} characters;</li>
 *   <li>not in a short blocklist of the most-guessed strings;</li>
 *   <li>not a single repeated character ({@code aaaaaaaaaaaa}), which passes a
 *       length check while containing almost no entropy.</li>
 * </ol>
 *
 * <p><b>What is deliberately not here:</b> checking against a real breach corpus
 * (that is a large download, or a call to an API), and telling the user which
 * rule they failed in a machine-readable way - today the message lists all
 * violations at once, which is what a form needs.
 */
public final class PasswordPolicy {

    /** Twelve is a floor, not a target: every extra character multiplies the work. */
    public static final int MIN_LENGTH = 12;

    /**
     * The shortest list that catches what real users type. A production system
     * would load thousands of entries from a file or a breached-password service;
     * the mechanism is identical, the size is not.
     */
    private static final Set<String> BLOCKLIST = Set.of(
            "password", "password1", "password123", "passw0rd",
            "123456", "12345678", "123456789", "qwerty", "qwertyuiop",
            "letmein", "welcome", "admin", "iloveyou", "changeme");

    private PasswordPolicy() {
        // rules only
    }

    /**
     * @return every rule the password breaks, in a stable order; empty means
     *         acceptable. A list rather than a boolean so the caller can tell the
     *         user everything at once instead of one failure per attempt.
     */
    public static List<String> violations(String password) {
        List<String> problems = new ArrayList<>(3);
        if (password == null || password.length() < MIN_LENGTH) {
            problems.add("must be at least " + MIN_LENGTH + " characters long");
        }
        if (password != null && BLOCKLIST.contains(password.toLowerCase(Locale.ROOT))) {
            problems.add("must not be a commonly used password");
        }
        if (password != null && password.length() >= MIN_LENGTH && isOneRepeatedCharacter(password)) {
            problems.add("must not be the same character repeated");
        }
        return problems;
    }

    private static boolean isOneRepeatedCharacter(String password) {
        char first = password.charAt(0);
        for (int i = 1; i < password.length(); i++) {
            if (password.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }
}
