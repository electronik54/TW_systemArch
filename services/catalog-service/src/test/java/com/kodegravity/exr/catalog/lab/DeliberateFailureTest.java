package com.kodegravity.exr.catalog.lab;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A test that is <b>supposed to fail</b>. It lives only on the throwaway branch
 * {@code feature/gate-lab} and must never be merged.
 *
 * <p>Why it exists: a branch protection rule, a red X and a blocked merge are
 * all <i>claims</i> until you have watched them happen. This file makes the
 * pipeline fail on purpose, so the whole path can be inspected once:
 *
 * <ol>
 *   <li>the failure is reported, not swallowed by {@code -B -ntp};</li>
 *   <li>the job exits non-zero and the check turns red on the pull request;</li>
 *   <li>the {@code Upload test reports on failure} step actually uploads
 *       {@code surefire-reports} — a failure path nobody tests is a failure path
 *       that quietly breaks;</li>
 *   <li>the red check blocks the merge button (once protection is enabled).</li>
 * </ol>
 *
 * <p>It is a <b>plain</b> JUnit test on purpose: no Spring context, no database,
 * no container. It fails in milliseconds, so the only thing being measured is the
 * pipeline itself — if this test tried to start Postgres, a green run could be
 * confused with "the database worked".
 */
class DeliberateFailureTest {

    @Test
    void thisTestExistsToProveTheCiGateBites() {
        int eventsPublishedThisWeek = 3;

        assertThat(eventsPublishedThisWeek)
                .as("deliberate failure - remove this file before merging")
                .isEqualTo(4);
    }
}
