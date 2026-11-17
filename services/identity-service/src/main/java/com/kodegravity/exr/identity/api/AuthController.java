package com.kodegravity.exr.identity.api;

import com.kodegravity.exr.identity.application.UserAccountService;
import com.kodegravity.exr.identity.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * The registration door. Deliberately thin: it translates JSON, calls one
 * service method, and maps the result to a status code. No hashing, no
 * validation logic, no repository.
 *
 * <p><b>Why the response is a record and not the {@link User} entity.</b> An
 * entity is a live database object with a version counter and a password hash on
 * it. Serialising it would publish the hash and tie the API's shape to the table's
 * shape, so a column rename would become a breaking API change.
 *
 * <p><b>What it returns:</b> the id, the email as stored, and the role - enough
 * for a client to show "welcome", and nothing that could be replayed or cracked.
 * The status is {@code 201 Created} with a {@code Location} header, because the
 * request created a resource and a client may want to fetch it later.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAccountService accounts;

    public AuthController(UserAccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/register")
    public ResponseEntity<Registered> register(@RequestBody RegisterRequest request) {
        User user = accounts.register(request.email(), request.password());

        return ResponseEntity
                .created(URI.create("/api/users/" + user.getId()))
                .body(new Registered(user.getId(), user.getEmail(), user.getRole().name()));
    }

    /** What a client may send. No id, no role: the server decides those. */
    public record RegisterRequest(String email, String password) {
    }

    /** What a client gets back. No hash, ever. */
    public record Registered(UUID id, String email, String role) {
    }
}
