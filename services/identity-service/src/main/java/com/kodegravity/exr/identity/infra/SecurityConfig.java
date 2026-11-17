package com.kodegravity.exr.identity.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The security policy for this service, written down instead of inherited.
 *
 * <p><b>What existed before this class.</b> With {@code spring-boot-starter-security}
 * on the classpath and no configuration, Boot applied its own chain: every URL
 * required authentication, a browser got a generated login form, and a single
 * user called {@code user} was created with a password printed at startup. That
 * is a safe default and a useless product — nothing can be public, and the
 * password changes on every restart.
 *
 * <p><b>Why a {@code SecurityFilterChain} bean and not
 * {@code WebSecurityConfigurerAdapter}.</b> The adapter, and its
 * {@code configure(HttpSecurity)} override, was removed in Spring Security 6.
 * Most tutorials online still show it; copying them produces code that does not
 * compile on Boot 3. The bean below is the supported form, and the lambda
 * arguments are the modern DSL.
 *
 * <p><b>Why this class lives in {@code infra}.</b> It configures a framework,
 * not a business rule. Nothing in {@code domain} should know that HTTP exists.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Ordered rules, first match wins. Read it top to bottom as the request's
     * journey: the first rule that matches decides, and the remaining ones never
     * run.
     */
    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http,
                                    RestAuthenticationEntryPoint entryPoint) throws Exception {
        http
                /*
                 * CSRF OFF, and the reason is not laziness.
                 *
                 * CSRF attacks work by making the BROWSER send a request with
                 * credentials it sends automatically - a session cookie. This API is
                 * stateless: no cookie is issued, and credentials arrive in an
                 * Authorization header that a foreign page cannot add. So the attack
                 * has no vehicle, and the token would only break every POST.
                 *
                 * The moment a cookie or a server-side session is introduced, this
                 * line becomes a vulnerability - which is exactly why the rule is
                 * written here rather than forgotten.
                 */
                .csrf(AbstractHttpConfigurer::disable)

                /*
                 * No sessions at all. Each request must prove who it is, on its own.
                 * That is what lets ten instances serve the same user without shared
                 * state - the property every horizontal-scaled service depends on.
                 */
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                /*
                 * The policy. `/public/**` is open; everything else needs a valid
                 * identity. There is deliberately NO rule that says "except the API",
                 * because a default-open rule is how internal endpoints end up on the
                 * internet.
                 */
                .authorizeHttpRequests(auth -> auth
                        // Open: the health-style probe, and the registration/login doors.
                        // Every opening here is a decision, which is why it is written
                        // as an explicit list rather than a pattern that could quietly
                        // grow to cover more than intended.
                        .requestMatchers("/api/public/**", "/api/auth/**").permitAll()
                        .anyRequest().authenticated())

                /*
                 * WHAT A REFUSAL LOOKS LIKE. Without this line, Spring Security
                 * writes its own 401: empty body, no correlation id - a client
                 * cannot tell it apart from a dropped connection. The entry point
                 * produces our RFC 7807 shape instead, with the id attached.
                 */
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint))

                /*
                 * TEMPORARY, until Day 5. HTTP Basic lets us prove authentication
                 * works without inventing tokens yet: the browser prompts, the
                 * credentials go in a header, and the same filter that will later read
                 * a JWT reads them. Basic auth sends the password on EVERY request,
                 * which is why it does not survive into production.
                 */
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))

                /* The generated login PAGE is turned off: this is an API, not a site. */
                .formLogin(AbstractHttpConfigurer::disable)
                
                ;

        return http.build();
    }

    /**
     * The one component that ever sees a plaintext password: it turns it into a
     * BCrypt digest, and later compares a typed password with a stored digest.
     *
     * <p><b>Why BCrypt and not MD5/SHA-256.</b> A fast hash is the wrong tool for
     * passwords. SHA-256 is designed to be quick, so a stolen table can be
     * attacked at billions of guesses per second on a GPU. BCrypt is deliberately
     * slow, and its cost factor is a knob: the default here (10) means one hash
     * costs tens of milliseconds - unnoticeable once at login, ruinous multiplied
     * by millions of guesses. Raising it to 12 is roughly four times slower, which
     * is why the number is chosen by measuring on real hardware, never by copying
     * a tutorial.
     *
     * <p><b>The salt is inside the digest.</b> BCrypt generates a random salt per
     * call and stores it in the output string, so two users with the same password
     * get different hashes and one cracked hash teaches an attacker nothing about
     * the next. Nothing extra to manage, nothing to keep secret.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
