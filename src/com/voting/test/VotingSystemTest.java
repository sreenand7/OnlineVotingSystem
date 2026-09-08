package com.voting.test;

import com.voting.manager.VotingManager;
import com.voting.model.Candidate;
import com.voting.model.Vote;
import com.voting.model.Voter;
import com.voting.util.VotingException;
import com.voting.util.VotingException.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Self-contained unit/integration test suite for the Online Voting System.
 *
 * <p>No external test framework is required. Run with:</p>
 * <pre>
 *   java -cp out com.voting.test.VotingSystemTest
 * </pre>
 *
 * <p>Each test prints PASS or FAIL with a description so failures are easy
 * to diagnose.</p>
 */
public class VotingSystemTest {

    // ── State ─────────────────────────────────────────────────────────────────

    private static int passed = 0;
    private static int failed = 0;

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   VOTING SYSTEM – TEST SUITE             ║");
        System.out.println("╚══════════════════════════════════════════╝\n");

        testVoterModel();
        testCandidateModel();
        testVoteModel();
        testSuccessfulVote();
        testAlreadyVoted();
        testVoterNotFound();
        testCandidateNotFound();
        testElectionNotYetOpen();
        testElectionAlreadyClosed();
        testTallyAccuracy();
        testExportCreatesFile();
        testDuplicateRegistrationIdempotent();
        testTieDetection();
        testVoterConstructorValidation();
        testCandidateConstructorValidation();
        testManagerWindowValidation();

        System.out.println("\n" + "═".repeat(44));
        System.out.printf("  Results: %d passed, %d failed (total %d)%n",
                passed, failed, passed + failed);
        System.out.println("═".repeat(44));
        System.exit(failed > 0 ? 1 : 0);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Model tests
    // ══════════════════════════════════════════════════════════════════════════

    private static void testVoterModel() {
        section("Voter model");

        Voter v = new Voter("V001", "Alice");
        assertEquals("Initial hasVoted should be false", false, v.hasVoted());
        v.markAsVoted();
        assertEquals("hasVoted after markAsVoted", true, v.hasVoted());
        assertEquals("getVoterId", "V001", v.getVoterId());
        assertEquals("getName", "Alice", v.getName());
    }

    private static void testCandidateModel() {
        section("Candidate model");

        Candidate c = new Candidate("C001", "Bob", "Liberty");
        assertEquals("getCandidateId", "C001", c.getCandidateId());
        assertEquals("getName", "Bob", c.getName());
        assertEquals("getPoliticalParty", "Liberty", c.getPoliticalParty());
    }

    private static void testVoteModel() {
        section("Vote model");

        Voter v     = new Voter("V002", "Carol");
        Candidate c = new Candidate("C002", "Dan", "Progress");
        LocalDateTime t = LocalDateTime.now();
        Vote vote   = new Vote(v, c, t);

        assertEquals("getVoter", v, vote.getVoter());
        assertEquals("getCandidate", c, vote.getCandidate());
        assertEquals("getTimestamp", t, vote.getTimestamp());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VotingManager – happy path
    // ══════════════════════════════════════════════════════════════════════════

    private static void testSuccessfulVote() {
        section("Successful vote");

        VotingManager mgr = openElection();
        registerBasicSetup(mgr);

        try {
            Vote v = mgr.castVote("V001", "C001");
            assertNotNull("Returned vote is not null", v);
            assertEquals("Voter marked as voted", true,
                    mgr.getVoters().stream()
                       .filter(v2 -> v2.getVoterId().equals("V001"))
                       .findFirst().orElseThrow().hasVoted());
            assertEquals("Vote log size is 1", 1, mgr.getVotes().size());
        } catch (VotingException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VotingManager – rejection paths
    // ══════════════════════════════════════════════════════════════════════════

    private static void testAlreadyVoted() {
        section("Duplicate vote rejected");

        VotingManager mgr = openElection();
        registerBasicSetup(mgr);

        try {
            mgr.castVote("V001", "C001");       // first vote – should succeed
            mgr.castVote("V001", "C001");        // second vote – should throw
            fail("Expected AlreadyVotedException");
        } catch (AlreadyVotedException e) {
            pass("AlreadyVotedException thrown correctly");
        } catch (VotingException e) {
            fail("Wrong exception type: " + e.getClass().getSimpleName());
        }
    }

    private static void testVoterNotFound() {
        section("Unknown voter rejected");

        VotingManager mgr = openElection();
        registerBasicSetup(mgr);

        try {
            mgr.castVote("V999", "C001");
            fail("Expected VoterNotFoundException");
        } catch (VoterNotFoundException e) {
            pass("VoterNotFoundException thrown correctly");
        } catch (VotingException e) {
            fail("Wrong exception type: " + e.getClass().getSimpleName());
        }
    }

    private static void testCandidateNotFound() {
        section("Unknown candidate rejected");

        VotingManager mgr = openElection();
        registerBasicSetup(mgr);

        try {
            mgr.castVote("V001", "C999");
            fail("Expected CandidateNotFoundException");
        } catch (CandidateNotFoundException e) {
            pass("CandidateNotFoundException thrown correctly");
        } catch (VotingException e) {
            fail("Wrong exception type: " + e.getClass().getSimpleName());
        }
    }

    private static void testElectionNotYetOpen() {
        section("Vote before election opens rejected");

        // Election starts in the future
        VotingManager mgr = new VotingManager("Future Election",
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2));
        registerBasicSetup(mgr);

        try {
            mgr.castVote("V001", "C001");
            fail("Expected ElectionClosedException");
        } catch (ElectionClosedException e) {
            pass("ElectionClosedException (not yet open) thrown correctly");
        } catch (VotingException e) {
            fail("Wrong exception type: " + e.getClass().getSimpleName());
        }
    }

    private static void testElectionAlreadyClosed() {
        section("Vote after election closed rejected");

        // Election ended in the past
        VotingManager mgr = new VotingManager("Past Election",
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1));
        registerBasicSetup(mgr);

        try {
            mgr.castVote("V001", "C001");
            fail("Expected ElectionClosedException");
        } catch (ElectionClosedException e) {
            pass("ElectionClosedException (already closed) thrown correctly");
        } catch (VotingException e) {
            fail("Wrong exception type: " + e.getClass().getSimpleName());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Tally & export
    // ══════════════════════════════════════════════════════════════════════════

    private static void testTallyAccuracy() {
        section("Vote tally accuracy");

        VotingManager mgr = openElection();
        // 3 candidates, 4 voters
        mgr.registerCandidate(new Candidate("C001", "Anna", "A"));
        mgr.registerCandidate(new Candidate("C002", "Ben",  "B"));
        mgr.registerCandidate(new Candidate("C003", "Cara", "C"));
        for (int i = 1; i <= 6; i++) {
            mgr.registerVoter(new Voter("V00" + i, "Voter " + i));
        }

        try {
            mgr.castVote("V001", "C001"); // Anna: 1
            mgr.castVote("V002", "C001"); // Anna: 2
            mgr.castVote("V003", "C001"); // Anna: 3
            mgr.castVote("V004", "C002"); // Ben:  1
            mgr.castVote("V005", "C002"); // Ben:  2
            // V006 doesn't vote – C003 gets 0
        } catch (VotingException e) {
            fail("Unexpected exception during tally test: " + e.getMessage());
            return;
        }

        Map<String, Long> tally = mgr.getTallyMap();
        assertEquals("C001 tally", 3L, tally.get("C001"));
        assertEquals("C002 tally", 2L, tally.get("C002"));
        assertEquals("C003 tally", 0L, tally.get("C003"));
        assertEquals("Total votes", 5, mgr.getVotes().size());
    }

    private static void testExportCreatesFile() {
        section("Export creates file");

        VotingManager mgr = openElection();
        registerBasicSetup(mgr);
        try {
            mgr.castVote("V001", "C001");
        } catch (VotingException e) {
            fail("Setup vote failed: " + e.getMessage());
            return;
        }

        String path = "test_export_output.txt";
        try {
            mgr.exportResults(path);
            File f = new File(path);
            assertTrue("Export file exists", f.exists());
            assertTrue("Export file is non-empty", f.length() > 0);
            f.delete(); // clean up
        } catch (IOException e) {
            fail("Export threw IOException: " + e.getMessage());
        }
    }

    private static void testDuplicateRegistrationIdempotent() {
        section("Duplicate registration is idempotent");

        VotingManager mgr = openElection();
        Voter v = new Voter("V001", "Alice");
        mgr.registerVoter(v);
        mgr.registerVoter(v); // second registration of same ID
        long count = mgr.getVoters().stream()
                .filter(vv -> vv.getVoterId().equals("V001"))
                .count();
        assertEquals("Only one voter with V001", 1L, count);
    }

    private static void testTieDetection() {
        section("Tie scenario (no crash in displayResults)");

        VotingManager mgr = openElection();
        mgr.registerCandidate(new Candidate("C001", "Ann", "X"));
        mgr.registerCandidate(new Candidate("C002", "Bob", "Y"));
        mgr.registerVoter(new Voter("V001", "V1"));
        mgr.registerVoter(new Voter("V002", "V2"));

        try {
            mgr.castVote("V001", "C001");
            mgr.castVote("V002", "C002");
        } catch (VotingException e) {
            fail("Unexpected exception: " + e.getMessage());
            return;
        }

        Map<String, Long> tally = mgr.getTallyMap();
        assertEquals("C001 tied at 1", 1L, tally.get("C001"));
        assertEquals("C002 tied at 1", 1L, tally.get("C002"));
        pass("Tie scenario handled correctly");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Constructor validation
    // ══════════════════════════════════════════════════════════════════════════

    private static void testVoterConstructorValidation() {
        section("Voter constructor validation");

        assertThrows("Blank voterId", IllegalArgumentException.class,
                () -> new Voter("", "Name"));
        assertThrows("Null voterId", IllegalArgumentException.class,
                () -> new Voter(null, "Name"));
        assertThrows("Blank name", IllegalArgumentException.class,
                () -> new Voter("V001", ""));
    }

    private static void testCandidateConstructorValidation() {
        section("Candidate constructor validation");

        assertThrows("Blank candidateId", IllegalArgumentException.class,
                () -> new Candidate("", "Name", "Party"));
        assertThrows("Blank party", IllegalArgumentException.class,
                () -> new Candidate("C001", "Name", ""));
    }

    private static void testManagerWindowValidation() {
        section("VotingManager window validation");

        assertThrows("Start == End", IllegalArgumentException.class,
                () -> {
                    LocalDateTime t = LocalDateTime.now();
                    new VotingManager("Bad", t, t);
                });
        assertThrows("Start after End", IllegalArgumentException.class,
                () -> new VotingManager("Bad",
                        LocalDateTime.now().plusHours(1),
                        LocalDateTime.now()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════════

    /** Returns a VotingManager whose window is open right now. */
    private static VotingManager openElection() {
        return new VotingManager("Test Election",
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().plusMinutes(10));
    }

    private static void registerBasicSetup(VotingManager mgr) {
        mgr.registerCandidate(new Candidate("C001", "Alice", "Party A"));
        mgr.registerCandidate(new Candidate("C002", "Bob",   "Party B"));
        mgr.registerVoter(new Voter("V001", "Carol"));
        mgr.registerVoter(new Voter("V002", "Dave"));
    }

    // ── Assertion helpers ─────────────────────────────────────────────────────

    private static void section(String name) {
        System.out.println("\n  ── " + name + " ──");
    }

    private static void assertEquals(String label, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            pass(label + " [expected: " + expected + "]");
        } else {
            fail(label + " [expected: " + expected + ", got: " + actual + "]");
        }
    }

    private static void assertTrue(String label, boolean condition) {
        if (condition) pass(label); else fail(label);
    }

    private static void assertNotNull(String label, Object obj) {
        if (obj != null) pass(label); else fail(label + " (was null)");
    }

    private static <T extends Throwable> void assertThrows(
            String label, Class<T> expected, Runnable action) {
        try {
            action.run();
            fail(label + " – expected " + expected.getSimpleName() + " but none thrown");
        } catch (Throwable t) {
            if (expected.isInstance(t)) {
                pass(label + " – " + expected.getSimpleName() + " thrown");
            } else {
                fail(label + " – expected " + expected.getSimpleName()
                        + " but got " + t.getClass().getSimpleName());
            }
        }
    }

    private static void pass(String label) {
        System.out.println("    PASS  " + label);
        passed++;
    }

    private static void fail(String label) {
        System.out.println("    FAIL  " + label);
        failed++;
    }
}
