package com.kodegravity.exr.identity.domain;

/**
 * The one business rule V1's unique index enforces: one account per email.
 *
 * <p>Thrown when the database refuses an INSERT, which is the only place the rule
 * can be applied safely - a Java check reads, then writes, and between those two
 * moments another request can insert the same address.
 *
 * <p><b>Its message names the email on purpose.</b> Registration has to reveal
 * whether an address is taken, otherwise the user cannot proceed. Login is the
 * opposite case: it must never say "no such user" versus "wrong password",
 * because that difference tells an attacker which addresses are real. Same
 * system, two different answers - decide per endpoint, never once for all.
 */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("An account already exists for " + email);
    }
}
