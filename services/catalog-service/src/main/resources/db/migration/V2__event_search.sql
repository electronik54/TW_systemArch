-- ============================================================================
--  V2 — full-text search
--  ---------------------------------------------------------------------------
--  Adds ONE derived column and ONE index. No table is hand-edited: the column is
--  COMPUTED by Postgres from columns that already exist, so the search data can
--  never disagree with the rows it describes.
--
--  The decision (Day 2.6, H3): Postgres `tsvector` + GIN — not `LIKE`, not
--  `pg_trgm`, not OpenSearch. Reasons, in the order that decided it:
--    1. `LIKE '%term%'` cannot use a B-tree at all — a leading wildcard leaves
--       nothing to sort on, so every search is a full scan of `events`.
--    2. `pg_trgm` CAN index that wildcard, but it indexes *characters*: "runn"
--       finds "running" only by sharing three-letter chunks, not because the two
--       words are the same word. It also has no notion of relevance, so ranking
--       the results is left to the application.
--    3. A `tsvector` is a *lexeme list* — the text already split into words,
--       lower-cased and stemmed ("running" → "run") — so it matches on the
--       meaning of words, carries positions for phrase search, and `ts_rank`
--       turns those into a number to sort by.
--    4. OpenSearch is the right tool at a scale this project does not have: a
--       second system to run, feed and keep consistent. Its day is Day 16; the
--       migration path is this same column, exported.
--
--  Flyway rules (unchanged since V1): forward only, one transaction, no
--  dependency on application code. V1 is now FROZEN — its checksum is recorded
--  in `flyway_schema_history`, and editing it makes every existing database
--  refuse to start with "Migration checksum mismatch".
-- ============================================================================


-- ---------------------------------------------------------------------------
-- 1. the column — generated, so it cannot drift
-- ---------------------------------------------------------------------------
--  WHY GENERATED, NOT A TRIGGER OR APPLICATION CODE
--  A plain `tsvector` filled by a trigger (or by Java) is a second copy of the
--  truth, and copies go stale: one forgotten code path, one bulk
--  `UPDATE ... SET synopsis = NULL` during the nightly purge, and the row is
--  searchable by text it no longer has. `GENERATED ALWAYS AS (...) STORED` puts
--  the maintenance inside the storage engine: any INSERT or UPDATE touching
--  `title`, `title_override` or `synopsis` recomputes the vector in the same
--  statement. No code path can forget.
--
--  WHY THE TWO-ARGUMENT `to_tsvector('cfg', text)`
--  The one-argument form reads `default_text_search_config` at run time, so it
--  is STABLE, not IMMUTABLE — and a generated column may only use immutable
--  expressions, because its stored value has to stay correct forever. Naming the
--  config makes the call immutable and the migration legal. The failure it
--  avoids is explicit: "generation expression is not immutable".
--
--  WHY TWO CONFIGS
--    'simple'  — lower-cases and splits; NO stemming, NO stopword list.
--                Used for the TITLE, because a stopword list silently deletes
--                whole titles: under 'english' a film called "It" is indexed as
--                the empty vector and becomes unfindable.
--    'english' — also drops stopwords and stems ("running" → "run").
--                Used for the SYNOPSIS, which is prose: nobody searches for
--                "the", and people do expect "run" to find "running".
--  The cost is honest: a search asks two questions instead of one ('simple' for
--  the title, 'english' for the synopsis), and the ranking takes the better of
--  the two. One OR inside the query, paid at read time.
--
--  WHY setweight
--  A match in the title is a better answer than a match deep in the synopsis.
--  setweight labels each lexeme position A/B/C/D, and `ts_rank` weighs those at
--  1.0 / 0.4 / 0.2 / 0.1 by default — so the film called "It" outranks the word
--  "it" inside somebody's plot summary.
--
--  WHY COALESCE(title_override, title)
--  The effective title is the admin's decision first (D-015): the index must
--  answer with the title the UI shows, not the raw supplier one. The `synopsis`
--  argument needs its empty-string fallback because `to_tsvector` returns NULL
--  for NULL input and `NULL || anything` is NULL — the whole column would be
--  NULL instead of "no searchable words".
ALTER TABLE events
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (
              setweight(to_tsvector('simple',  coalesce(title_override, title)), 'A')
           || setweight(to_tsvector('english', coalesce(synopsis, '')),          'B')
        ) STORED;

--  Visible in `\d+ events` and to anyone reading the schema later: this column is
--  the database's, not the application's.
COMMENT ON COLUMN events.search_vector IS
    'Lexemes of the effective title (weight A) and effective synopsis (weight B), '
    'maintained by Postgres. Never written by the application.';


-- ---------------------------------------------------------------------------
-- 2. the index — GIN is the inverted index
-- ---------------------------------------------------------------------------
--  `search_vector @@ query` asks "does this document contain these words?". A
--  B-tree cannot answer that: it sorts whole values, and a vector is a SET of
--  words, so there is no single order to seek on. GIN (generalized inverted
--  index) stores the opposite mapping — word → the rows containing it — which is
--  the shape of the question, so one lookup returns the candidates and Postgres
--  then re-checks each candidate against the real vector. That recheck is by
--  design: a GIN index is lossy, so it can return extra rows to look at, never a
--  wrong row in the answer.
--
--  THE TRADE: GIN costs more per write than a B-tree — one index entry per
--  DISTINCT lexeme in the row. Accepted here because `events` is read far more
--  often than written: a search runs on every keystroke of an autocomplete, an
--  event is ingested once.
--
--  WHAT IT DOES **NOT** DO: it filters, it never sorts. `ORDER BY ts_rank(...)`
--  is a computation over the rows that survived the index scan, so a GIN search
--  is "narrow first, then rank" — never "walk the index in relevance order".
CREATE INDEX events_search_gin
    ON events USING gin (search_vector);


-- ---------------------------------------------------------------------------
-- 3. what this costs here, and what it would cost on a BIG table
-- ---------------------------------------------------------------------------
--  `events` is empty or tiny today, so this is instant. On a real table it is
--  not: adding a STORED generated column REWRITES every row under an ACCESS
--  EXCLUSIVE lock — reads and writes, and in effect the service, stop for the
--  duration. The zero-downtime route (the answer to "how would you ship this at
--  50 million rows?") is:
--      a) ADD the column as a plain nullable tsvector   (no rewrite);
--      b) fill it in batches: UPDATE ... WHERE search_vector IS NULL;
--      c) CREATE INDEX CONCURRENTLY                     (no write lock);
--      d) keep it filled by a trigger, or convert to GENERATED (another
--         rewrite — which is why the mature version stays with the trigger).
--  The cheap version and the safe version are different migrations; a small
--  table is what makes the cheap one honest today.
--
--  The other edge: a `tsvector` cannot exceed 1 MB. `synopsis` is unbounded
--  `text`, so a pathological blurb — hundreds of kilobytes of distinct words —
--  would make the INSERT fail rather than store an unindexable row. If a source
--  ever ships one, index `left(synopsis, 20000)` instead.
--
--  Try it by hand — the same shape the repository query will use:
--      SELECT id, coalesce(title_override, title) AS title,
--             greatest(ts_rank(search_vector, websearch_to_tsquery('simple',  :term)),
--                      ts_rank(search_vector, websearch_to_tsquery('english', :term))) AS score
--      FROM events
--      WHERE search_vector @@ websearch_to_tsquery('simple',  :term)
--         OR search_vector @@ websearch_to_tsquery('english', :term)
--      ORDER BY score DESC
--      LIMIT 20;

