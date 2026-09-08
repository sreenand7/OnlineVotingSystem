package com.voting.manager;

import com.voting.model.Candidate;
import com.voting.model.Vote;
import com.voting.model.Voter;
import com.voting.util.VotingException;
import com.voting.util.VotingException.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Central manager for the online voting system.
 *
 * Responsibilities:
 * - Register voters and candidates.
 * - Enforce the election time window.
 * - Authenticate voters and record votes (one per voter).
 * - Compute and display election results.
 * - Export a human-readable summary report to a text file.
 *
 * Lifecycle: configure -> register candidates/voters -> start -> vote -> results.
 *
 * Thread-safety: all mutating methods are synchronized so that
 * concurrent vote submissions are handled safely.
 */

public class VotingManager {

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Election metadata ─────────────────────────────────────────────────────

    private final String electionName;
    private final int    durationMinutes;

    /** Set when {@link #startElection()} is called. */
    private LocalDateTime electionStart;
    private LocalDateTime electionEnd;

    // ── Registries ────────────────────────────────────────────────────────────

    /** Key: voterId */
    private final Map<String, Voter>     voters     = new LinkedHashMap<>();
    /** Key: candidateId */
    private final Map<String, Candidate> candidates = new LinkedHashMap<>();

    /** Ordered log of every accepted ballot. */
    private final List<Vote> votes = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Constructs a VotingManager with a deferred voting window.
     *
     * <p>The election is <em>not</em> open yet — call {@link #startElection()}
     * after all candidates and voters have been registered.</p>
     *
     * @param electionName    display name of this election
     * @param durationMinutes how many minutes the voting window lasts once started
     * @throws IllegalArgumentException if name is blank or duration ≤ 0
     */
    public VotingManager(String electionName, int durationMinutes) {
        if (electionName == null || electionName.isBlank()) {
            throw new IllegalArgumentException("Election name must not be null or blank.");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0 minutes.");
        }
        this.electionName    = electionName.trim();
        this.durationMinutes = durationMinutes;
        // electionStart / electionEnd remain null until startElection()
    }

    /**
     * Constructs a VotingManager with an explicit, already-active time window.
     *
     * <p>This constructor is kept for backward compatibility with the test
     * suite, where elections must be open (or closed) immediately.</p>
     *
     * @param electionName  display name of this election
     * @param electionStart first moment at which votes are accepted (inclusive)
     * @param electionEnd   last moment at which votes are accepted (inclusive)
     * @throws IllegalArgumentException if start is not strictly before end
     */
    public VotingManager(String electionName,
                         LocalDateTime electionStart,
                         LocalDateTime electionEnd) {
        if (electionStart == null || electionEnd == null) {
            throw new IllegalArgumentException("Election start and end must not be null.");
        }
        if (!electionStart.isBefore(electionEnd)) {
            throw new IllegalArgumentException(
                    "Election start must be strictly before election end.");
        }
        this.electionName    = electionName;
        this.durationMinutes = (int) java.time.Duration.between(electionStart, electionEnd).toMinutes();
        this.electionStart   = electionStart;
        this.electionEnd     = electionEnd;
    }

    // ── Election lifecycle ────────────────────────────────────────────────────

    /**
     * Starts the election. The voting window begins <em>now</em> and lasts
     * for the configured duration.
     *
     * @throws IllegalStateException if the election has already been started
     */
    public synchronized void startElection() {
        if (electionStart != null) {
            throw new IllegalStateException("Election has already been started.");
        }
        this.electionStart = LocalDateTime.now();
        this.electionEnd   = electionStart.plusMinutes(durationMinutes);
    }

    /** Returns {@code true} if {@link #startElection()} has been invoked. */
    public synchronized boolean isElectionStarted() {
        return electionStart != null;
    }

    /**
     * Returns {@code true} if the election has been started <em>and</em>
     * the current time is within the voting window.
     */
    public synchronized boolean isElectionOpen() {
        if (electionStart == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(electionStart) && !now.isAfter(electionEnd);
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Registers a voter. Duplicate IDs are silently ignored (idempotent).
     *
     * @param voter the voter to register
     */
    public synchronized void registerVoter(Voter voter) {
        Objects.requireNonNull(voter, "Voter must not be null.");
        voters.putIfAbsent(voter.getVoterId(), voter);
    }

    /**
     * Registers a candidate. Duplicate IDs are silently ignored (idempotent).
     *
     * @param candidate the candidate to register
     */
    public synchronized void registerCandidate(Candidate candidate) {
        Objects.requireNonNull(candidate, "Candidate must not be null.");
        candidates.putIfAbsent(candidate.getCandidateId(), candidate);
    }

    // ── Voting ────────────────────────────────────────────────────────────────

    /**
     * Casts a vote on behalf of {@code voterId} for {@code candidateId}.
     *
     * <p>Validation sequence (order matters for meaningful error messages):</p>
     * <ol>
     *   <li>Election must have been started.</li>
     *   <li>Voter must be registered.</li>
     *   <li>Candidate must be registered.</li>
     *   <li>Current time must be within the election window.</li>
     *   <li>Voter must not have already voted.</li>
     * </ol>
     *
     * @param voterId     ID of the voter casting the ballot
     * @param candidateId ID of the candidate being voted for
     * @return the recorded {@link Vote}
     * @throws VoterNotFoundException      if voterId is unknown
     * @throws CandidateNotFoundException  if candidateId is unknown
     * @throws ElectionClosedException     if outside the election window
     * @throws AlreadyVotedException       if the voter already voted
     */
    public synchronized Vote castVote(String voterId, String candidateId)
            throws VotingException {

        // 0. Election must have been started
        if (electionStart == null) {
            throw new ElectionClosedException("Election has not been started yet.");
        }

        // 1. Look up voter
        Voter voter = voters.get(voterId);
        if (voter == null) throw new VoterNotFoundException(voterId);

        // 2. Look up candidate
        Candidate candidate = candidates.get(candidateId);
        if (candidate == null) throw new CandidateNotFoundException(candidateId);

        // 3. Time-window check
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(electionStart)) {
            throw new ElectionClosedException(
                    "Election opens at " + electionStart.format(DISPLAY_FMT));
        }
        if (now.isAfter(electionEnd)) {
            throw new ElectionClosedException(
                    "Election closed at " + electionEnd.format(DISPLAY_FMT));
        }

        // 4. Duplicate-vote check
        if (voter.hasVoted()) throw new AlreadyVotedException(voterId);

        // All checks passed – record the vote
        Vote vote = new Vote(voter, candidate, now);
        votes.add(vote);
        voter.markAsVoted();

        System.out.printf("[VOTE RECORDED] Voter '%s' voted for '%s' (%s) at %s%n",
                voter.getName(),
                candidate.getName(),
                candidate.getPoliticalParty(),
                now.format(DISPLAY_FMT));

        return vote;
    }

    // ── Results ───────────────────────────────────────────────────────────────

    /**
     * Returns an unmodifiable, descending-by-vote-count ordered map of
     * {@code candidateId → voteCount}.
     */
    public synchronized Map<String, Long> getTallyMap() {
        // Count votes per candidate
        Map<String, Long> tally = new LinkedHashMap<>();
        candidates.keySet().forEach(id -> tally.put(id, 0L));
        for (Vote v : votes) {
            tally.merge(v.getCandidate().getCandidateId(), 1L, Long::sum);
        }
        // Sort descending
        return tally.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * Prints a formatted election result table to {@code System.out}.
     */
    public synchronized void displayResults() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.printf ("║  ELECTION RESULTS: %-43s║%n", electionName);
        System.out.println("╠══════════╦══════════════════════════╦════════════╦════════════╣");
        System.out.println("║    ID    ║         Candidate        ║   Party    ║   Votes    ║");
        System.out.println("╠══════════╬══════════════════════════╬════════════╬════════════╣");

        long totalVotes = votes.size();
        Map<String, Long> tally = getTallyMap();

        for (Map.Entry<String, Long> entry : tally.entrySet()) {
            Candidate c     = candidates.get(entry.getKey());
            long      count = entry.getValue();
            double    pct   = totalVotes == 0 ? 0 : 100.0 * count / totalVotes;
            System.out.printf("║ %-8s ║ %-24s ║ %-10s ║ %4d(%5.1f%%) ║%n",
                    c.getCandidateId(),
                    truncate(c.getName(), 24),
                    truncate(c.getPoliticalParty(), 10),
                    count, pct);
        }

        System.out.println("╠══════════╩══════════════════════════╩════════════╩════════════╣");
        System.out.printf ("║  Total votes cast: %-43d║%n", totalVotes);
        System.out.printf ("║  Total voters registered: %-36d║%n", voters.size());
        System.out.printf ("║  Turnout: %-52s║%n",
                voters.isEmpty() ? "N/A"
                        : String.format("%.1f%%", 100.0 * totalVotes / voters.size()));

        // Determine winner(s)
        if (!tally.isEmpty()) {
            long maxVotes = tally.values().iterator().next();
            List<String> winners = tally.entrySet().stream()
                    .filter(e -> e.getValue() == maxVotes)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            if (winners.size() == 1) {
                Candidate w = candidates.get(winners.get(0));
                System.out.printf("║  ★ Winner: %-51s║%n",
                        w.getName() + " (" + w.getPoliticalParty() + ")");
            } else {
                System.out.printf("║  ★ TIE between: %-45s║%n",
                        winners.stream()
                               .map(id -> candidates.get(id).getName())
                               .collect(Collectors.joining(", ")));
            }
        }
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    // ── Export ────────────────────────────────────────────────────────────────

    /**
     * Exports the election summary report to {@code filePath}.
     *
     * <p>The file is UTF-8 encoded plain text. If the file already exists it
     * is overwritten.</p>
     *
     * @param filePath destination file path
     * @throws IOException if the file cannot be written
     */
    public synchronized void exportResults(String filePath) throws IOException {
        Objects.requireNonNull(filePath, "File path must not be null.");

        Map<String, Long> tally      = getTallyMap();
        long              totalVotes = votes.size();
        String            generatedAt = LocalDateTime.now().format(DISPLAY_FMT);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            // Header
            writeLine(bw, "=".repeat(66));
            writeLine(bw, "  ELECTION SUMMARY REPORT");
            writeLine(bw, "=".repeat(66));
            writeLine(bw, "  Election  : " + electionName);
            writeLine(bw, "  Opens     : " + (electionStart != null
                    ? electionStart.format(DISPLAY_FMT) : "Not started"));
            writeLine(bw, "  Closes    : " + (electionEnd != null
                    ? electionEnd.format(DISPLAY_FMT) : "Not started"));
            writeLine(bw, "  Generated : " + generatedAt);
            writeLine(bw, "-".repeat(66));

            // Results table
            writeLine(bw, String.format("  %-8s  %-26s  %-14s  %-8s  %s",
                    "ID", "Candidate", "Party", "Votes", "Share"));
            writeLine(bw, "-".repeat(66));

            for (Map.Entry<String, Long> entry : tally.entrySet()) {
                Candidate c     = candidates.get(entry.getKey());
                long      count = entry.getValue();
                double    pct   = totalVotes == 0 ? 0 : 100.0 * count / totalVotes;
                writeLine(bw, String.format("  %-8s  %-26s  %-14s  %-8d  %.1f%%",
                        c.getCandidateId(),
                        c.getName(),
                        c.getPoliticalParty(),
                        count, pct));
            }

            writeLine(bw, "-".repeat(66));
            writeLine(bw, "  Total votes cast        : " + totalVotes);
            writeLine(bw, "  Total voters registered : " + voters.size());
            writeLine(bw, "  Voter turnout           : " +
                    (voters.isEmpty() ? "N/A"
                            : String.format("%.1f%%", 100.0 * totalVotes / voters.size())));

            // Winner
            if (!tally.isEmpty()) {
                long maxVotes = tally.values().iterator().next();
                List<String> winners = tally.entrySet().stream()
                        .filter(e -> e.getValue() == maxVotes)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                bw.newLine();
                if (winners.size() == 1) {
                    Candidate w = candidates.get(winners.get(0));
                    writeLine(bw, "  ★ WINNER: " + w.getName()
                            + " (" + w.getPoliticalParty() + ")"
                            + " with " + maxVotes + " vote(s)");
                } else {
                    writeLine(bw, "  ★ TIE: " +
                            winners.stream()
                                   .map(id -> candidates.get(id).getName())
                                   .collect(Collectors.joining(", ")));
                }
            }

            writeLine(bw, "=".repeat(66));

            // Detailed vote log
            bw.newLine();
            writeLine(bw, "  DETAILED VOTE LOG");
            writeLine(bw, "-".repeat(66));
            if (votes.isEmpty()) {
                writeLine(bw, "  No votes were cast.");
            } else {
                writeLine(bw, String.format("  %-10s  %-20s  %-14s  %s",
                        "VoterID", "Voter", "CandidateID", "Timestamp"));
                writeLine(bw, "-".repeat(66));
                for (Vote v : votes) {
                    writeLine(bw, String.format("  %-10s  %-20s  %-14s  %s",
                            v.getVoter().getVoterId(),
                            v.getVoter().getName(),
                            v.getCandidate().getCandidateId(),
                            v.getTimestamp().format(DISPLAY_FMT)));
                }
            }
            writeLine(bw, "=".repeat(66));
        }

        System.out.println("[EXPORT] Results written to: " + filePath);
    }

    // ── Accessors (for testing/inspection) ───────────────────────────────────

    /** Returns an unmodifiable view of all registered voters. */
    public synchronized Collection<Voter> getVoters() {
        return Collections.unmodifiableCollection(voters.values());
    }

    /** Returns an unmodifiable view of all registered candidates. */
    public synchronized Collection<Candidate> getCandidates() {
        return Collections.unmodifiableCollection(candidates.values());
    }

    /** Returns an unmodifiable view of all recorded votes. */
    public synchronized List<Vote> getVotes() {
        return Collections.unmodifiableList(votes);
    }

    public String        getElectionName()    { return electionName; }
    public int           getDurationMinutes() { return durationMinutes; }
    public LocalDateTime getElectionStart()   { return electionStart; }
    public LocalDateTime getElectionEnd()     { return electionEnd; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void writeLine(BufferedWriter bw, String line) throws IOException {
        bw.write(line);
        bw.newLine();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }
}
