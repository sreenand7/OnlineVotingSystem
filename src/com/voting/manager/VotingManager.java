package com.voting.manager;

import com.voting.dao.CandidateDAO;
import com.voting.dao.ElectionDAO;
import com.voting.dao.VoteDAO;
import com.voting.dao.VoterDAO;
import com.voting.model.Candidate;
import com.voting.model.Vote;
import com.voting.model.Voter;
import com.voting.util.VotingException;
import com.voting.util.VotingException.AlreadyVotedException;
import com.voting.util.VotingException.CandidateNotFoundException;
import com.voting.util.VotingException.ElectionClosedException;
import com.voting.util.VotingException.VoterNotFoundException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Central manager for the online voting system, scoped to a single election.
 *
 * <p><b>MySQL is the single source of truth.</b> Unlike earlier versions of
 * this class, {@code VotingManager} holds <em>no</em> in-memory registries of
 * voters, candidates, or votes. Every read and write goes through
 * {@link ElectionDAO}, {@link VoterDAO}, {@link CandidateDAO}, and
 * {@link VoteDAO}, keyed by this instance's {@link #electionId}. This means
 * results are always consistent with the database, and multiple
 * {@code VotingManager} instances (or other processes) can safely observe
 * the same election concurrently.</p>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Create or attach to a single election row.</li>
 *   <li>Register voters and candidates for that election.</li>
 *   <li>Enforce the election time window and status.</li>
 *   <li>Authenticate voters and record votes (one per voter per election).</li>
 *   <li>Compute and display election results.</li>
 *   <li>Export a human-readable summary report to a text file.</li>
 * </ul>
 *
 * <p>Lifecycle: construct (creates or attaches to an election row) -&gt;
 * register candidates/voters -&gt; {@link #startElection()} -&gt;
 * {@link #castVote(String, String)} -&gt; results/export. When resuming an
 * {@code UPCOMING} election attached via {@link #VotingManager(int)}, use
 * {@link #startElection(int)} instead, since its duration was never set at
 * creation time.</p>
 *
 * <p>Re-elections: pass a {@code parentElectionId} to
 * {@link #VotingManager(String, int, Integer)} to create a new election row
 * linked to a prior one via {@code parent_election_id}. Each
 * {@code VotingManager} always represents exactly one election row; a
 * re-election is a brand-new instance wrapping a brand-new row.</p>
 *
 * <p>Thread-safety: all mutating/reading methods are synchronized on this
 * instance so that concurrent calls through the <em>same</em>
 * {@code VotingManager} object are serialized. Because the database itself
 * enforces the real invariants (e.g. the {@code UNIQUE (election_id, voter_id)}
 * constraint on {@code votes}), correctness does not depend on this alone —
 * it also holds across separate instances/processes.</p>
 */
public class VotingManager {

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── DAOs ─────────────────────────────────────────────────────────────────

    private final ElectionDAO electionDAO = new ElectionDAO();
    private final VoterDAO voterDAO = new VoterDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final VoteDAO voteDAO = new VoteDAO();

    // ── Election identity ────────────────────────────────────────────────────

    /** Primary key of the {@code elections} row this manager operates on. */
    private final int electionId;

    /**
     * Voting-window length in minutes, used by {@link #startElection()} to
     * compute the real end time. Not persisted as its own column (the
     * database stores {@code start_time}/{@code end_time} directly), so it
     * is kept here only as a convenience for the deferred-start workflow.
     *
     * <p>For elections created via the deferred-start constructors, this is
     * known immediately. For an election attached via
     * {@link #VotingManager(int)} while still {@code UPCOMING}, the
     * database only holds placeholder start/end timestamps, so there is no
     * real duration to derive — this field is left at {@code 0} until it is
     * supplied explicitly via {@link #startElection(int)}. For an election
     * attached while {@code ACTIVE} or {@code COMPLETED}, the row's real
     * timestamps are meaningful, so the duration is derived from them as
     * before.</p>
     */
    private int durationMinutes;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Creates a brand-new election with a deferred voting window (status
     * {@code UPCOMING}). Call {@link #startElection()} once all candidates
     * and voters have been registered.
     *
     * @param electionName    display name of this election
     * @param durationMinutes how many minutes the voting window lasts once started
     * @throws IllegalArgumentException if name is blank or duration &le; 0
     * @throws SQLException             if the election row cannot be created
     */
    public VotingManager(String electionName, int durationMinutes) throws SQLException {
        this(electionName, durationMinutes, null);
    }

    /**
     * Creates a brand-new election, optionally as a re-election of a prior
     * one (linked via {@code parent_election_id}). Status starts
     * {@code UPCOMING}; call {@link #startElection()} to open voting.
     *
     * @param electionName     display name of this election
     * @param durationMinutes  how many minutes the voting window lasts once started
     * @param parentElectionId the original election's ID if this is a
     *                         re-election, or {@code null} otherwise
     * @throws IllegalArgumentException if name is blank or duration &le; 0
     * @throws SQLException             if the election row cannot be created
     */
    public VotingManager(String electionName, int durationMinutes, Integer parentElectionId)
            throws SQLException {
        if (electionName == null || electionName.isBlank()) {
            throw new IllegalArgumentException("Election name must not be null or blank.");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0 minutes.");
        }
        this.durationMinutes = durationMinutes;
        // Placeholder start/end: start_time/end_time are NOT NULL, but the
        // real voting window is only known once startElection() is called.
        LocalDateTime placeholder = LocalDateTime.now();
        this.electionId = electionDAO.createElection(
                electionName.trim(), placeholder, placeholder, parentElectionId);
    }

    /**
     * Creates a brand-new election with an explicit, already-active time
     * window, bypassing the deferred-start workflow. Status is set to
     * {@code ACTIVE} immediately.
     *
     * <p>Kept for test suites that need an election open (or already
     * closed) as soon as it is constructed.</p>
     *
     * @param electionName  display name of this election
     * @param electionStart first moment at which votes are accepted (inclusive)
     * @param electionEnd   last moment at which votes are accepted (inclusive)
     * @throws IllegalArgumentException if start is not strictly before end
     * @throws SQLException             if the election row cannot be created
     */
    public VotingManager(String electionName, LocalDateTime electionStart, LocalDateTime electionEnd)
            throws SQLException {
        if (electionName == null || electionName.isBlank()) {
            throw new IllegalArgumentException("Election name must not be null or blank.");
        }
        if (electionStart == null || electionEnd == null) {
            throw new IllegalArgumentException("Election start and end must not be null.");
        }
        if (!electionStart.isBefore(electionEnd)) {
            throw new IllegalArgumentException(
                    "Election start must be strictly before election end.");
        }
        this.durationMinutes = (int) java.time.Duration.between(electionStart, electionEnd).toMinutes();
        this.electionId = electionDAO.createElection(electionName.trim(), electionStart, electionEnd);
        electionDAO.updateStatus(electionId, ElectionDAO.Status.ACTIVE);
    }

    /**
     * Attaches to an already-existing election row (e.g. to resume
     * operations on it, inspect results, or reference it as the parent of a
     * later re-election).
     *
     * <p>If the election is still {@code UPCOMING}, its {@code start_time}/
     * {@code end_time} are just placeholders (no real duration has been
     * decided yet), so {@link #getDurationMinutes()} will read {@code 0}
     * until it is supplied via {@link #startElection(int)}. If the election
     * is {@code ACTIVE} or {@code COMPLETED}, its timestamps are real, so
     * the duration is derived from them immediately.</p>
     *
     * @param electionId the primary key of an existing {@code elections} row
     * @throws IllegalArgumentException if no election with that ID exists
     * @throws SQLException             if the lookup fails
     */
    public VotingManager(int electionId) throws SQLException {
        ElectionDAO.ElectionRecord record = electionDAO.findById(electionId);
        if (record == null) {
            throw new IllegalArgumentException("No election found with ID " + electionId);
        }
        this.electionId = electionId;
        this.durationMinutes = "UPCOMING".equals(record.status)
                ? 0
                : (int) java.time.Duration.between(record.startTime, record.endTime).toMinutes();
    }

    // ── Election lifecycle ────────────────────────────────────────────────────

    /**
     * Starts the election. The voting window begins <em>now</em> and lasts
     * for the configured duration; status moves to {@code ACTIVE}.
     *
     * <p>Use this overload when the duration is already known — i.e. the
     * election was created via {@link #VotingManager(String, int)} /
     * {@link #VotingManager(String, int, Integer)}, or was attached via
     * {@link #VotingManager(int)} while already {@code ACTIVE}/
     * {@code COMPLETED}. If this instance is attached to an {@code UPCOMING}
     * election whose duration was never set, use
     * {@link #startElection(int)} instead.</p>
     *
     * @throws IllegalStateException if the election has already been started,
     *                                or no duration has been set yet
     * @throws SQLException          if the update fails
     */
    public synchronized void startElection() throws SQLException {
        if (durationMinutes <= 0) {
            throw new IllegalStateException(
                    "No voting duration is set for this election yet. "
                    + "Use startElection(int durationMinutes) to supply one.");
        }
        doStartElection(durationMinutes);
    }

    /**
     * Starts the election with an explicitly supplied duration, moving
     * status to {@code ACTIVE}. This is the entry point for resuming an
     * {@code UPCOMING} election that was attached via
     * {@link #VotingManager(int)}, whose stored {@code start_time}/
     * {@code end_time} are only placeholders and so carry no real duration.
     *
     * @param durationMinutes how many minutes the voting window lasts, starting now
     * @throws IllegalArgumentException if duration &le; 0
     * @throws IllegalStateException    if the election has already been started
     * @throws SQLException             if the update fails
     */
    public synchronized void startElection(int durationMinutes) throws SQLException {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0 minutes.");
        }
        doStartElection(durationMinutes);
    }

    /**
     * Shared implementation for {@link #startElection()} and
     * {@link #startElection(int)}: validates the election is still
     * {@code UPCOMING}, persists the real voting window, and records the
     * duration on this instance so later calls (and
     * {@link #getDurationMinutes()}) reflect it.
     */
    private void doStartElection(int minutes) throws SQLException {
        ElectionDAO.ElectionRecord record = requireElection();
        if (!"UPCOMING".equals(record.status)) {
            throw new IllegalStateException("Election has already been started.");
        }
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime end = now.plusMinutes(minutes);
        electionDAO.startElection(electionId, now, end);
        this.durationMinutes = minutes;
    }

    /** Returns {@code true} if {@link #startElection()} has been invoked. */
    public synchronized boolean isElectionStarted() throws SQLException {
        ElectionDAO.ElectionRecord record = requireElection();
        return !"UPCOMING".equals(record.status);
    }

    /**
     * Returns {@code true} if the election has been started, is not yet
     * completed, and the current time is within the voting window.
     */
    public synchronized boolean isElectionOpen() throws SQLException {
        ElectionDAO.ElectionRecord record = requireElection();
        LocalDateTime now = LocalDateTime.now();
        boolean open = "ACTIVE".equals(record.status)
                && !now.isBefore(record.startTime)
                && !now.isAfter(record.endTime);

        return open;
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Registers a voter globally (if not already registered) and enrolls
     * them into this election (if not already enrolled). Idempotent, like
     * the previous in-memory {@code putIfAbsent} behavior.
     *
     * @param voter the voter to register
     * @throws SQLException if the voter ID is not a valid integer, or a
     *                       database operation fails
     */public synchronized void registerVoter(Voter voter)
        throws SQLException, VoterNotFoundException {
    
        Objects.requireNonNull(voter, "Voter must not be null.");
        int voterId = parseVoterId(voter.getVoterId());

        if (!voterDAO.voterExists(voterId)) {
            voterDAO.registerVoter(voterId, voter.getName());
        }

        boolean alreadyEnrolled = voterDAO.getVotersForElection(electionId).stream()
                .anyMatch(v -> v.getVoterId().equals(String.valueOf(voterId)));
        if (!alreadyEnrolled) {
            voterDAO.addVoterToElection(electionId, voterId);
        }
    }

    /**
     * Registers a new candidate for this election.
     *
     * <p>Unlike the old in-memory version, this does <em>not</em> accept a
     * pre-built {@link Candidate}: {@code candidate_id} is generated by the
     * database on insert, so there is no valid ID to put on a {@code Candidate}
     * object beforehand. Call this method instead, and use the
     * {@link Candidate} it returns (which carries the real, generated ID) for
     * any subsequent calls such as {@link #castVote(String, String)}.</p>
     *
     * @param name           candidate's full name
     * @param politicalParty candidate's political party
     * @return the persisted {@link Candidate}, with its generated ID
     * @throws SQLException if the insert fails
     */
    public synchronized Candidate registerCandidate(String name, String politicalParty)
            throws SQLException {
        Objects.requireNonNull(name, "Candidate name must not be null.");
        Objects.requireNonNull(politicalParty, "Candidate party must not be null.");
        int candidateId = candidateDAO.addCandidate(electionId, name, politicalParty);
        return new Candidate(String.valueOf(candidateId), name, politicalParty);
    }

    // ── Voting ────────────────────────────────────────────────────────────────

    /**
     * Casts a vote on behalf of {@code voterId} for {@code candidateId} in
     * this election.
     *
     * <p>Validation sequence (order matters for meaningful error messages):</p>
     * <ol>
     *   <li>Election must have been started.</li>
     *   <li>Voter must be registered (globally known to the system).</li>
     *   <li>Candidate must be registered in <em>this</em> election.</li>
     *   <li>Current time must be within the election window.</li>
     *   <li>Voter must not have already voted in this election.</li>
     * </ol>
     *
     * @param voterId     ID of the voter casting the ballot
     * @param candidateId ID of the candidate being voted for
     * @return the recorded {@link Vote}
     * @throws VoterNotFoundException     if voterId is unknown or not a valid ID
     * @throws CandidateNotFoundException if candidateId is unknown in this election
     * @throws ElectionClosedException    if outside the election window
     * @throws AlreadyVotedException      if the voter already voted in this election
     * @throws SQLException               if a database operation fails
     */
    public synchronized Vote castVote(String voterId, String candidateId)
            throws VotingException, SQLException {

        // 0. Election must have been started, and not yet completed
        ElectionDAO.ElectionRecord record = requireElection();
        if ("UPCOMING".equals(record.status)) {
            throw new ElectionClosedException("Election has not been started yet.");
        }

        // 1. Look up voter (must exist globally)
        int voterIntId = parseOrNotFound(voterId, true);
        Voter voter = voterDAO.findByVoterId(voterIntId);
        if (voter == null) throw new VoterNotFoundException(voterId);

        // 2. Look up candidate (must exist in this election)
        int candidateIntId = parseOrNotFound(candidateId, false);
        Candidate candidate = candidateDAO.findByCandidateId(candidateIntId);
        if (candidate == null || !candidateDAO.candidateExists(electionId, candidateIntId)) {
            throw new CandidateNotFoundException(candidateId);
        }

        // 3. Time-window check
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(record.startTime)) {
            throw new ElectionClosedException(
                    "Election opens at " + record.startTime.format(DISPLAY_FMT));
        }
        if (now.isAfter(record.endTime) || "COMPLETED".equals(record.status)) {
            throw new ElectionClosedException(
                    "Election closed at " + record.endTime.format(DISPLAY_FMT));
        }

        // 4. Duplicate-vote check (voter must be enrolled in this election)
        boolean alreadyVoted;
        try {
            alreadyVoted = voterDAO.hasVoted(electionId, voterIntId);
        } catch (SQLException e) {
            // hasVoted() throws when there is no election_voters row at all,
            // i.e. the voter was never enrolled in this election.
            throw new VoterNotFoundException(voterId);
        }
        if (alreadyVoted) throw new AlreadyVotedException(voterId);

        // All checks passed – record the vote
        voteDAO.saveVote(electionId, voterIntId, candidateIntId);
        voterDAO.markAsVoted(electionId, voterIntId);

        Vote vote = new Vote(voter, candidate, now);

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
     * {@code candidateId → voteCount} for this election.
     *
     * @throws SQLException if the query fails
     */
    public synchronized Map<String, Long> getTallyMap() throws SQLException {
        List<Candidate> electionCandidates = candidateDAO.getCandidatesForElection(electionId);
        Map<Integer, Integer> counts = voteDAO.countVotesByCandidate(electionId);

        Map<String, Long> tally = new LinkedHashMap<>();
        for (Candidate c : electionCandidates) {
            int id = Integer.parseInt(c.getCandidateId());
            tally.put(c.getCandidateId(), (long) counts.getOrDefault(id, 0));
        }

        return tally.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * Prints a formatted election result table to {@code System.out}.
     *
     * @throws SQLException if a query fails
     */
    public synchronized void displayResults() throws SQLException {
        ElectionDAO.ElectionRecord record = requireElection();
        Map<String, Candidate> candidates = candidatesById();
        Map<String, Long> tally = getTallyMap();
        long totalVotes = tally.values().stream().mapToLong(Long::longValue).sum();
        int totalVoters = voterDAO.getVotersForElection(electionId).size();

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.printf ("║  ELECTION RESULTS: %-43s║%n", record.name);
        System.out.println("╠══════════╦══════════════════════════╦════════════╦════════════╣");
        System.out.println("║    ID    ║         Candidate        ║   Party    ║   Votes    ║");
        System.out.println("╠══════════╬══════════════════════════╬════════════╬════════════╣");

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
        System.out.printf ("║  Total voters registered: %-36d║%n", totalVoters);
        System.out.printf ("║  Turnout: %-52s║%n",
                totalVoters == 0 ? "N/A"
                        : String.format("%.1f%%", 100.0 * totalVotes / totalVoters));

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
     * @throws IOException  if the file cannot be written
     * @throws SQLException if a query fails
     */
    public synchronized void exportResults(String filePath) throws IOException, SQLException {
        Objects.requireNonNull(filePath, "File path must not be null.");

        ElectionDAO.ElectionRecord record = requireElection();
        Map<String, Candidate> candidates = candidatesById();
        Map<String, Long> tally = getTallyMap();
        List<Vote> voteLog = voteDAO.getVotesForElection(electionId);
        long totalVotes = tally.values().stream().mapToLong(Long::longValue).sum();
        int totalVoters = voterDAO.getVotersForElection(electionId).size();
        String generatedAt = LocalDateTime.now().format(DISPLAY_FMT);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            // Header
            writeLine(bw, "=".repeat(66));
            writeLine(bw, "  ELECTION SUMMARY REPORT");
            writeLine(bw, "=".repeat(66));
            writeLine(bw, "  Election  : " + record.name);
            writeLine(bw, "  Opens     : " + record.startTime.format(DISPLAY_FMT));
            writeLine(bw, "  Closes    : " + record.endTime.format(DISPLAY_FMT));
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
            writeLine(bw, "  Total voters registered : " + totalVoters);
            writeLine(bw, "  Voter turnout           : " +
                    (totalVoters == 0 ? "N/A"
                            : String.format("%.1f%%", 100.0 * totalVotes / totalVoters)));

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
            if (voteLog.isEmpty()) {
                writeLine(bw, "  No votes were cast.");
            } else {
                writeLine(bw, String.format("  %-10s  %-20s  %-14s  %s",
                        "VoterID", "Voter", "CandidateID", "Timestamp"));
                writeLine(bw, "-".repeat(66));
                for (Vote v : voteLog) {
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

    /** Returns all voters enrolled in this election. */
    public synchronized Collection<Voter> getVoters() throws SQLException {
        return Collections.unmodifiableCollection(voterDAO.getVotersForElection(electionId));
    }

    /** Returns all candidates registered in this election. */
    public synchronized Collection<Candidate> getCandidates() throws SQLException {
        return Collections.unmodifiableCollection(candidateDAO.getCandidatesForElection(electionId));
    }

    /** Returns all votes cast in this election. */
    public synchronized List<Vote> getVotes() throws SQLException {
        return Collections.unmodifiableList(voteDAO.getVotesForElection(electionId));
    }

    /** Returns the primary key of the {@code elections} row this manager operates on. */
    public int getElectionId() { return electionId; }

    public synchronized String getElectionName() throws SQLException {
        return requireElection().name;
    }

    /**
     * Returns the voting-window duration in minutes.
     *
     * @return the duration, or {@code 0} if this instance is attached to an
     *         {@code UPCOMING} election whose duration hasn't been supplied
     *         yet via {@link #startElection(int)}
     */
    public int getDurationMinutes() { return durationMinutes; }

    public synchronized LocalDateTime getElectionStart() throws SQLException {
        return requireElection().startTime;
    }

    public synchronized LocalDateTime getElectionEnd() throws SQLException {
        return requireElection().endTime;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ElectionDAO.ElectionRecord requireElection() throws SQLException {
        ElectionDAO.ElectionRecord record = electionDAO.findById(electionId);
        if (record == null) {
            throw new IllegalStateException("Election " + electionId + " no longer exists.");
        }
        return record;
    }

    private Map<String, Candidate> candidatesById() throws SQLException {
        Map<String, Candidate> map = new LinkedHashMap<>();
        for (Candidate c : candidateDAO.getCandidatesForElection(electionId)) {
            map.put(c.getCandidateId(), c);
        }
        return map;
    }

    private int parseVoterId(String voterId) throws VoterNotFoundException {
        try {
            return Integer.parseInt(voterId);
        } catch (NumberFormatException e) {
            throw new VoterNotFoundException(voterId);
        }
    }

    /**
     * Parses a voter or candidate ID string, throwing the appropriate
     * "not found" exception (rather than a raw {@link NumberFormatException})
     * if it is not a valid integer.
     */
    private int parseOrNotFound(String id, boolean isVoter) throws VotingException {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            if (isVoter) throw new VoterNotFoundException(id);
            throw new CandidateNotFoundException(id);
        }
    }

    private static void writeLine(BufferedWriter bw, String line) throws IOException {
        bw.write(line);
        bw.newLine();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }
}
