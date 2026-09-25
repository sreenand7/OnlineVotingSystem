package com.voting;

import static com.voting.VotingException.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class VotingManager {

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ElectionDAO electionDAO = new ElectionDAO();
    private final VoterDAO voterDAO = new VoterDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final VoteDAO voteDAO = new VoteDAO();

    private final int electionId;
    private int durationMinutes;

    // Creates a new election (UPCOMING status)
    public VotingManager(String electionName, int durationMinutes) throws SQLException {
        this(electionName, durationMinutes, null);
    }

    // Creates a new election, optionally as a re-election of a parent
    public VotingManager(String electionName, int durationMinutes, Integer parentElectionId)
            throws SQLException {
        if (electionName == null || electionName.isBlank())
            throw new IllegalArgumentException("Election name must not be null or blank.");
        if (durationMinutes <= 0)
            throw new IllegalArgumentException("Duration must be greater than 0 minutes.");
        this.durationMinutes = durationMinutes;
        // Placeholder times — real window is set when startElection() is called
        LocalDateTime placeholder = LocalDateTime.now();
        this.electionId = electionDAO.createElection(
                electionName.trim(), placeholder, placeholder, parentElectionId);
    }

    // Creates an election with an explicit active time window (for testing)
    public VotingManager(String electionName, LocalDateTime electionStart, LocalDateTime electionEnd)
            throws SQLException {
        if (electionName == null || electionName.isBlank())
            throw new IllegalArgumentException("Election name must not be null or blank.");
        if (electionStart == null || electionEnd == null)
            throw new IllegalArgumentException("Election start and end must not be null.");
        if (!electionStart.isBefore(electionEnd))
            throw new IllegalArgumentException("Election start must be strictly before election end.");
        this.durationMinutes = (int) java.time.Duration.between(electionStart, electionEnd).toMinutes();
        this.electionId = electionDAO.createElection(electionName.trim(), electionStart, electionEnd);
        electionDAO.updateStatus(electionId, ElectionDAO.Status.ACTIVE);
    }

    // Attaches to an existing election row
    public VotingManager(int electionId) throws SQLException {
        ElectionDAO.ElectionRecord record = electionDAO.findById(electionId);
        if (record == null)
            throw new IllegalArgumentException("No election found with ID " + electionId);
        this.electionId = electionId;
        // UPCOMING elections have placeholder timestamps, so duration is unknown
        this.durationMinutes = "UPCOMING".equals(record.status)
                ? 0
                : (int) java.time.Duration.between(record.startTime, record.endTime).toMinutes();
    }

    // ── Election lifecycle ────────────────────────────────────────────────

    public synchronized void startElection() throws SQLException {
        if (durationMinutes <= 0)
            throw new IllegalStateException(
                    "No voting duration set. Use startElection(int durationMinutes).");
        doStartElection(durationMinutes);
    }

    public synchronized void startElection(int durationMinutes) throws SQLException {
        if (durationMinutes <= 0)
            throw new IllegalArgumentException("Duration must be greater than 0 minutes.");
        doStartElection(durationMinutes);
    }

    private void doStartElection(int minutes) throws SQLException {
        ElectionDAO.ElectionRecord record = requireElection();
        if (!"UPCOMING".equals(record.status))
            throw new IllegalStateException("Election has already been started.");
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime end = now.plusMinutes(minutes);
        electionDAO.startElection(electionId, now, end);
        this.durationMinutes = minutes;
    }

    public synchronized boolean isElectionStarted() throws SQLException {
        return !"UPCOMING".equals(requireElection().status);
    }

    public synchronized boolean isElectionOpen() throws SQLException {
        ElectionDAO.ElectionRecord record = requireElection();
        LocalDateTime now = LocalDateTime.now();
        return "ACTIVE".equals(record.status)
                && !now.isBefore(record.startTime)
                && !now.isAfter(record.endTime);
    }

    // ── Registration ──────────────────────────────────────────────────────

    public synchronized void registerVoter(Voter voter)
            throws SQLException, VoterNotFoundException {
        Objects.requireNonNull(voter, "Voter must not be null.");
        int voterId = parseVoterId(voter.getVoterId());

        if (!voterDAO.voterExists(voterId))
            voterDAO.registerVoter(voterId, voter.getName());

        boolean alreadyEnrolled = voterDAO.getVotersForElection(electionId).stream()
                .anyMatch(v -> v.getVoterId().equals(String.valueOf(voterId)));
        if (!alreadyEnrolled)
            voterDAO.addVoterToElection(electionId, voterId);
    }

    public synchronized int[] importVoters(List<Voter> voters) throws SQLException {
        int newlyRegistered = 0;
        int alreadyExisted  = 0;
        int alreadyEnrolled = 0;

        for (Voter voter : voters) {
            int voterId = Integer.parseInt(voter.getVoterId());
            if (!voterDAO.voterExists(voterId)) {
                voterDAO.registerVoter(voterId, voter.getName());
                newlyRegistered++;
            } else {
                alreadyExisted++;
            }
            if (!voterDAO.isVoterInElection(electionId, voterId)) {
                voterDAO.addVoterToElection(electionId, voterId);
            } else {
                alreadyEnrolled++;
            }
        }
        return new int[]{ newlyRegistered, alreadyExisted, alreadyEnrolled };
    }

    public synchronized Candidate registerCandidate(String name, String politicalParty)
            throws SQLException {
        Objects.requireNonNull(name, "Candidate name must not be null.");
        Objects.requireNonNull(politicalParty, "Candidate party must not be null.");
        int candidateId = candidateDAO.addCandidate(electionId, name, politicalParty);
        return new Candidate(String.valueOf(candidateId), name, politicalParty);
    }

    // ── Voting ────────────────────────────────────────────────────────────

    public synchronized Vote castVote(String voterId, String candidateId)
            throws VotingException, SQLException {

        ElectionDAO.ElectionRecord record = requireElection();
        if ("UPCOMING".equals(record.status))
            throw new ElectionClosedException("Election has not been started yet.");

        int voterIntId = parseOrNotFound(voterId, true);
        Voter voter = voterDAO.findByVoterId(voterIntId);
        if (voter == null) throw new VoterNotFoundException(voterId);

        int candidateIntId = parseOrNotFound(candidateId, false);
        Candidate candidate = candidateDAO.findByCandidateId(candidateIntId);
        if (candidate == null || !candidateDAO.candidateExists(electionId, candidateIntId))
            throw new CandidateNotFoundException(candidateId);

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(record.startTime))
            throw new ElectionClosedException("Election opens at " + record.startTime.format(DISPLAY_FMT));
        if (now.isAfter(record.endTime) || "COMPLETED".equals(record.status))
            throw new ElectionClosedException("Election closed at " + record.endTime.format(DISPLAY_FMT));

        // hasVoted() throws if voter is not enrolled in this election
        boolean alreadyVoted;
        try {
            alreadyVoted = voterDAO.hasVoted(electionId, voterIntId);
        } catch (SQLException e) {
            throw new VoterNotFoundException(voterId);
        }
        if (alreadyVoted) throw new AlreadyVotedException(voterId);

        voteDAO.saveVote(electionId, voterIntId, candidateIntId);
        voterDAO.markAsVoted(electionId, voterIntId);

        Vote vote = new Vote(voter, candidate, now);
        System.out.printf("[VOTE RECORDED] Voter '%s' voted for '%s' (%s) at %s%n",
                voter.getName(), candidate.getName(),
                candidate.getPoliticalParty(), now.format(DISPLAY_FMT));
        return vote;
    }

    // ── Results ───────────────────────────────────────────────────────────

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

    public synchronized void displayResults() throws SQLException {
        ElectionDAO.ElectionRecord record = requireElection();
        Map<String, Candidate> candidates = candidatesById();
        Map<String, Long> tally = getTallyMap();
        long totalVotes = tally.values().stream().mapToLong(Long::longValue).sum();
        int totalVoters = voterDAO.getVotersForElection(electionId).size();

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.printf ("║  ELECTION RESULTS: %-43s║%n", record.name);
        System.out.println("╠══════════╦══════════════════════════╦════════════╦════════════╣");
        System.out.println("║    ID    ║         Candidate        ║   Party    ║   Votes    ║");
        System.out.println("╠══════════╬══════════════════════════╬════════════╬════════════╣");

        for (Map.Entry<String, Long> entry : tally.entrySet()) {
            Candidate c     = candidates.get(entry.getKey());
            long      count = entry.getValue();
            double    pct   = totalVotes == 0 ? 0 : 100.0 * count / totalVotes;
            System.out.printf("║ %-8s ║ %-24s ║ %-10s ║ %4d(%5.1f%%)║%n",
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
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    // ── Export ────────────────────────────────────────────────────────────

    public synchronized void exportResults(String filePath) throws IOException, SQLException {
        Objects.requireNonNull(filePath, "File path must not be null.");

        ElectionDAO.ElectionRecord record = requireElection();
        Map<String, Candidate> candidates = candidatesById();
        Map<String, Long> tally = getTallyMap();
        List<Vote> voteLog = voteDAO.getVotesForElection(electionId);
        long totalVotes = tally.values().stream().mapToLong(Long::longValue).sum();
        int totalVoters = voterDAO.getVotersForElection(electionId).size();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            writeLine(bw, "=".repeat(66));
            writeLine(bw, "  ELECTION SUMMARY REPORT");
            writeLine(bw, "=".repeat(66));
            writeLine(bw, "  Election  : " + record.name);
            writeLine(bw, "  Opens     : " + record.startTime.format(DISPLAY_FMT));
            writeLine(bw, "  Closes    : " + record.endTime.format(DISPLAY_FMT));
            writeLine(bw, "  Generated : " + LocalDateTime.now().format(DISPLAY_FMT));
            writeLine(bw, "-".repeat(66));

            writeLine(bw, String.format("  %-8s  %-26s  %-14s  %-8s  %s",
                    "ID", "Candidate", "Party", "Votes", "Share"));
            writeLine(bw, "-".repeat(66));

            for (Map.Entry<String, Long> entry : tally.entrySet()) {
                Candidate c     = candidates.get(entry.getKey());
                long      count = entry.getValue();
                double    pct   = totalVotes == 0 ? 0 : 100.0 * count / totalVotes;
                writeLine(bw, String.format("  %-8s  %-26s  %-14s  %-8d  %.1f%%",
                        c.getCandidateId(), c.getName(),
                        c.getPoliticalParty(), count, pct));
            }

            writeLine(bw, "-".repeat(66));
            writeLine(bw, "  Total votes cast        : " + totalVotes);
            writeLine(bw, "  Total voters registered : " + totalVoters);
            writeLine(bw, "  Voter turnout           : " +
                    (totalVoters == 0 ? "N/A"
                            : String.format("%.1f%%", 100.0 * totalVotes / totalVoters)));

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

    // ── Accessors ─────────────────────────────────────────────────────────

    public synchronized Collection<Voter> getVoters() throws SQLException {
        return Collections.unmodifiableCollection(voterDAO.getVotersForElection(electionId));
    }

    public synchronized Collection<Candidate> getCandidates() throws SQLException {
        return Collections.unmodifiableCollection(candidateDAO.getCandidatesForElection(electionId));
    }

    public synchronized List<Vote> getVotes() throws SQLException {
        return Collections.unmodifiableList(voteDAO.getVotesForElection(electionId));
    }

    public int getElectionId() { return electionId; }

    public synchronized String getElectionName() throws SQLException {
        return requireElection().name;
    }

    public int getDurationMinutes() { return durationMinutes; }

    public synchronized LocalDateTime getElectionStart() throws SQLException {
        return requireElection().startTime;
    }

    public synchronized LocalDateTime getElectionEnd() throws SQLException {
        return requireElection().endTime;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private ElectionDAO.ElectionRecord requireElection() throws SQLException {
        ElectionDAO.ElectionRecord record = electionDAO.findById(electionId);
        if (record == null)
            throw new IllegalStateException("Election " + electionId + " no longer exists.");
        return record;
    }

    private Map<String, Candidate> candidatesById() throws SQLException {
        Map<String, Candidate> map = new LinkedHashMap<>();
        for (Candidate c : candidateDAO.getCandidatesForElection(electionId))
            map.put(c.getCandidateId(), c);
        return map;
    }

    private int parseVoterId(String voterId) throws VoterNotFoundException {
        try {
            return Integer.parseInt(voterId);
        } catch (NumberFormatException e) {
            throw new VoterNotFoundException(voterId);
        }
    }

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
