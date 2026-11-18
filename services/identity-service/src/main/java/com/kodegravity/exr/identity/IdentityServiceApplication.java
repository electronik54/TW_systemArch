package com.kodegravity.exr.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The identity service: users, credentials, account states, and — from Day 5 —
 * the tokens every other service trusts.
 *
 * <p>Why it is a separate service at all (`plan.md` §3): its load profile is the
 * opposite of the catalogue's. Writes are rare, reads are hot, and a mistake here
 * is a security incident rather than a wrong release date. Nothing is cached in
 * front of a token check, and there is no read replica to fall behind.
 *
 * <p>The package matters more than it looks: {@code @SpringBootApplication}
 * scans {@code com.kodegravity.exr.identity} and everything under it. A class
 * placed above this package - or a security config put somewhere clever - is
 * simply never found.
 */
@SpringBootApplication
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
