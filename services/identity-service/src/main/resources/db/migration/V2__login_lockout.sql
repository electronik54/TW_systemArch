-- ============================================================================
--  V2 - identity: failed login counting
-- ============================================================================
--  Day 4.3, brute-force lockout.
--
--  WHY A COUNTER AND A DEADLINE, NOT A FLAG
--  A permanent "locked" flag would let any stranger lock any account for ever by
--  typing wrong passwords - a denial-of-service gift, and a support queue full of
--  "I cannot log in". So the lock is temporary: the counter records how far along
--  an attack is, and `locked_until` records when the door reopens by itself.
--
--  WHY THE DEADLINE IS STORED AND NOT COMPUTED
--  Computing "last failure + 15 minutes" would need a per-attempt history table.
--  One timestamp answers the only question the login path asks: may this account
--  authenticate right now?
--
--  WHAT THIS DOES NOT STOP
--  A distributed attack with one guess per account across a million accounts
--  (credential stuffing). That is stopped by rate limiting per IP and by the
--  password blocklist, not by a per-account counter. Say so out loud in an
--  interview rather than pretending lockout is a complete answer.
-- ============================================================================

ALTER TABLE users
    ADD COLUMN failed_attempts smallint    NOT NULL DEFAULT 0,
    ADD COLUMN locked_until    timestamptz;

-- A counter that can go negative is a bug waiting to hide; the CHECK makes it loud.
ALTER TABLE users
    ADD CONSTRAINT users_failed_attempts_chk CHECK (failed_attempts >= 0);

COMMENT ON COLUMN users.failed_attempts IS
    'Consecutive failed logins since the last success. Reset to 0 on success.';
COMMENT ON COLUMN users.locked_until IS
    'Login is refused until this instant. NULL means not locked. Expires by itself '
    'so a stranger cannot lock an account permanently.';
