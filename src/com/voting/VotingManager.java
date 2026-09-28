package com.voting;

import static com.voting.VotingException.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class VotingManager {

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // DOB is entered as dd/MM/yyyy but the generated PIN uses MMDDYYYY
    public static final DateTimeFormatter DOB_INPUT_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter PIN_DOB_FMT =
            DateTimeFormatter.ofPattern("MMddyyyy");

    // DAOs are stateless helpers, so they are shared rather than per-election.
    // This lets authentication run before any election has been selected.
    private static final ElectionDAO  electionDAO  = new ElectionDAO();
    private static final VoterDAO    voterDAO    = new VoterDAO();
    private static final CandidateDAO candidateDAO = new CandidateDAO();
    private static final VoteDAO     voteDAO     = new VoteDAO();
    private static final AdminDAO    adminDAO    = new AdminDAO();

    private static final VoterSession session      = VoterSession.getInstance();
    private static final AdminSession adminSession = AdminSession.getInstance();

    private final int electionId;
    private int durationMinutes;

    // Creates a new election (UPCOMING status)
    public VotingManager(String electionName, int durationMinutes) throws SQLException {
        this(electionName, durationMinutes, null);
    }

    // Creates a new election, optionally as a re-election of a parent
    public VotingManager(String electionName, int durationMinutes, Integer parentElectionId)
            throws SQLException {
        requireAdmin();
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
        requireAdmin();
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

    // Attaches to an existing election row. Reachable by voters too, so no admin guard.
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
        requireAdmin();
        if (durationMinutes <= 0)
            throw new IllegalStateException(
                    "No voting duration set. Use startElection(int durationMinutes).");
        doStartElection(durationMinutes);
    }

    public synchronized void startElection(int durationMinutes) throws SQLException {
        requireAdmin();
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

    // Administrative action: ACTIVE -> COMPLETED.
    // Logout never does this, and voters have no way to reach it.
    public synchronized void completeElection() throws SQLException {
        requireAdmin();
        ElectionDAO.ElectionRecord record = requireElection();
        if (!"ACTIVE".equals(record.status))
            throw new IllegalStateException(
                    "Only an ACTIVE election can be completed (current status: "
                    + record.status + ").");
        electionDAO.updateStatus(electionId, ElectionDAO.Status.COMPLETED);
    }

    // ── Registration ──────────────────────────────────────────────────────

    // Registers a voter and enrols them in this election. A new voter gets an
    // initial PIN generated from their first name and date of birth.
    // Returns the generated plaintext PIN, or null if the voter already existed.
    public synchronized String registerVoter(Voter voter)
            throws SQLException, VoterNotFoundException {
        requireAdmin();
        requireUpcoming("add a voter to");
        Objects.requireNonNull(voter, "Voter must not be null.");
        int voterId = parseVoterId(voter.getVoterId());

        String generatedPin = null;
        if (!voterDAO.voterExists(voterId)) {
            if (!voter.hasDateOfBirth())
                throw new IllegalArgumentException(
                        "A date of birth is required to generate the voter's initial PIN.");
            generatedPin = generateInitialPin(voter.getName(), voter.getDateOfBirth());
            voterDAO.registerVoter(voterId, voter.getName(), voter.getDateOfBirth(),
                    PasswordHasher.hash(generatedPin));
        }

        boolean alreadyEnrolled = voterDAO.getVotersForElection(electionId).stream()
                .anyMatch(v -> v.getVoterId().equals(String.valueOf(voterId)));
        if (!alreadyEnrolled)
            voterDAO.addVoterToElection(electionId, voterId);

        return generatedPin;
    }

    public synchronized Candidate registerCandidate(String name, String politicalParty)
            throws SQLException {
        requireAdmin();
        requireUpcoming("add a candidate to");
        Objects.requireNonNull(name, "Candidate name must not be null.");
        Objects.requireNonNull(politicalParty, "Candidate party must not be null.");
        int candidateId = candidateDAO.addCandidate(electionId, name, politicalParty);
        return new Candidate(String.valueOf(candidateId), name, politicalParty);
    }

    // ── Authentication ────────────────────────────────────────────────────
    // Static: identifying a user must not depend on any election.

    public static VoterSession getSession() {
        return session;
    }

    public static void logout() {
        session.clear();
    }

    // Returns true on success, false on a wrong ID or password.
    public static boolean adminLogin(String adminId, String password) throws SQLException {
        if (adminId == null || adminId.isBlank() || password == null || password.isEmpty())
            return false;
        AdminDAO.AdminRecord admin = adminDAO.findAdmin(adminId.trim());
        if (admin == null) return false;
        if (!PasswordHasher.verify(password, admin.passwordHash)) return false;
        adminSession.start(admin.adminId);
        return true;
    }

    public static void adminLogout() {
        adminSession.clear();
    }

    public static boolean isAdminAuthenticated() {
        return adminSession.isLoggedIn();
    }

    public static String getAuthenticatedAdminId() {
        return adminSession.getAdminId();
    }

    // True when the voter exists and a PIN was generated for them
    public static boolean hasPin(String voterId) throws SQLException, VoterNotFoundException {
        Voter voter = voterDAO.findByVoterId(parseVoterIdStatic(voterId));
        if (voter == null) throw new VoterNotFoundException(voterId);
        return voter.hasPassword();
    }

    // Returns true on success, false on a wrong PIN. Throws if the voter does not exist.
    public static boolean login(String voterId, String pin)
            throws SQLException, VoterNotFoundException {
        int id = parseVoterIdStatic(voterId);
        Voter voter = voterDAO.findByVoterId(id);
        if (voter == null) throw new VoterNotFoundException(voterId);
        if (!voter.hasPassword()) return false;

        if (!PasswordHasher.verify(pin, voter.getPasswordHash()))
            return false;

        session.start(id, voter.getName());
        return true;
    }

    // Lets the logged-in voter replace their own PIN. The current PIN must verify first.
    // PINs are trimmed and lowercased to match the generated initial-PIN format,
    // and may mix letters and digits (minimum 4 characters).
    public static void changePin(String currentPin, String newPin, String confirmPin)
            throws SQLException, VotingException {
        if (!session.isLoggedIn())
            throw new IllegalStateException("No voter is logged in. Please log in first.");

        String current = normalizePin(currentPin);
        String fresh   = normalizePin(newPin);
        String confirm = normalizePin(confirmPin);

        if (fresh == null || fresh.length() < 4)
            throw new IllegalArgumentException("PIN must be at least 4 characters.");
        if (!fresh.equals(confirm))
            throw new IllegalArgumentException("New PIN and confirmation do not match.");

        int voterId = session.getVoterId();
        Voter voter = voterDAO.findByVoterId(voterId);
        if (voter == null || !voter.hasPassword())
            throw new VoterNotFoundException(String.valueOf(voterId));
        if (!PasswordHasher.verify(current, voter.getPasswordHash()))
            throw new VotingException("Current PIN is incorrect.");

        voterDAO.updatePasswordHash(voterId, PasswordHasher.hash(fresh));
    }

    private static String normalizePin(String pin) {
        return pin == null ? null : pin.trim().toLowerCase(Locale.ROOT);
    }

    // Active elections this logged-in voter is enrolled in and can still vote in
    public static List<ElectionDAO.VoterElection> getEligibleActiveElections() throws SQLException {
        if (!session.isLoggedIn())
            throw new IllegalStateException("No voter is logged in. Please log in first.");
        return electionDAO.getActiveElectionsForVoter(session.getVoterId());
    }

    // Parses dd/MM/yyyy; throws DateTimeParseException on bad input
    public static LocalDate parseDateOfBirth(String text) {
        return LocalDate.parse(text.trim(), DOB_INPUT_FMT);
    }

    // Initial PIN rule: first name (lowercase) + DOB as MMDDYYYY.
    // "Rahul Kumar" + 05/12/2000 -> "rahul05122000"
    public static String generateInitialPin(String name, LocalDate dateOfBirth) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Voter name must not be null or blank.");
        if (dateOfBirth == null)
            throw new IllegalArgumentException("Date of birth must not be null.");
        String firstName = name.trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
        return firstName + dateOfBirth.format(PIN_DOB_FMT);
    }

    private static void requireAdmin() {
        if (!adminSession.isLoggedIn())
            throw new IllegalStateException(
                    "Administrator login is required for this action.");
    }

    // Voter and candidate lists are frozen once voting starts. Only an UPCOMING
    // election can be edited — this is the real guard, not just the GUI buttons.
    private void requireUpcoming(String action) throws SQLException {
        String status = requireElection().status;
        if (!"UPCOMING".equals(status))
            throw new IllegalStateException("Cannot " + action + " a " + status
                    + " election. Only an UPCOMING election can be modified.");
    }

    // Final artefacts (results, exports) exist only once voting has closed.
    private void requireCompleted(String action) throws SQLException {
        String status = requireElection().status;
        if (!"COMPLETED".equals(status))
            throw new IllegalStateException("Cannot " + action + " a " + status
                    + " election. Results are available once the election is COMPLETED.");
    }

    public synchronized String getStatus() throws SQLException {
        return requireElection().status;
    }

    private static int parseVoterIdStatic(String voterId) throws VoterNotFoundException {
        try {
            return Integer.parseInt(voterId);
        } catch (NumberFormatException e) {
            throw new VoterNotFoundException(voterId);
        }
    }

    // Imports voters from a parsed CSV. Existing global voters are reused and never
    // duplicated. Returns counts plus the generated PINs for newly created voters.
    public synchronized ImportResult importVoters(List<Voter> voters) throws SQLException {
        requireAdmin();
        requireUpcoming("import voters into");
        int newlyRegistered = 0;
        int alreadyExisted  = 0;
        int alreadyEnrolled = 0;
        Map<String, String> generatedPins = new LinkedHashMap<>();

        for (Voter voter : voters) {
            int voterId = Integer.parseInt(voter.getVoterId());
            if (!voterDAO.voterExists(voterId)) {
                if (!voter.hasDateOfBirth())
                    throw new IllegalArgumentException(
                            "Voter " + voterId + " has no date of birth, so no initial PIN "
                            + "can be generated.");
                String pin = generateInitialPin(voter.getName(), voter.getDateOfBirth());
                voterDAO.registerVoter(voterId, voter.getName(), voter.getDateOfBirth(),
                        PasswordHasher.hash(pin));
                generatedPins.put(voter.getVoterId(), pin);
                newlyRegistered++;
            } else {
                alreadyExisted++;
                // Pre-existing voter with no PIN: the CSV DOB lets us issue one once.
                // An existing PIN is never overwritten.
                Voter existing = voterDAO.findByVoterId(voterId);
                if (existing != null && !existing.hasPassword() && voter.hasDateOfBirth()) {
                    String pin = generateInitialPin(voter.getName(), voter.getDateOfBirth());
                    voterDAO.updateVoterCredentials(voterId, voter.getDateOfBirth(),
                            PasswordHasher.hash(pin));
                    generatedPins.put(voter.getVoterId(), pin);
                }
            }
            if (!voterDAO.isVoterInElection(electionId, voterId)) {
                voterDAO.addVoterToElection(electionId, voterId);
            } else {
                alreadyEnrolled++;
            }
        }
        return new ImportResult(newlyRegistered, alreadyExisted, alreadyEnrolled, generatedPins);
    }

    public static final class ImportResult {
        public final int newlyRegistered;
        public final int alreadyExisted;
        public final int alreadyEnrolled;
        public final Map<String, String> generatedPins;

        ImportResult(int newlyRegistered, int alreadyExisted, int alreadyEnrolled,
                     Map<String, String> generatedPins) {
            this.newlyRegistered = newlyRegistered;
            this.alreadyExisted = alreadyExisted;
            this.alreadyEnrolled = alreadyEnrolled;
            this.generatedPins = generatedPins;
        }
    }

    // ── Voting ────────────────────────────────────────────────────────────

    // Casts a ballot as the currently logged-in voter.
    public synchronized Vote castVote(String candidateId) throws VotingException, SQLException {
        if (!session.isLoggedIn())
            throw new IllegalStateException("No voter is logged in. Please log in first.");
        return castVote(String.valueOf(session.getVoterId()), candidateId);
    }

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
        requireAdmin();
        requireCompleted("export results from");
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

    // ── Removal ───────────────────────────────────────────────────────────

    public synchronized void removeVoterFromElection(int voterId) throws SQLException {
        requireAdmin();
        ElectionDAO.ElectionRecord record = requireElection();
        if ("ACTIVE".equals(record.status))
            throw new IllegalStateException(
                    "Voters cannot be removed after the election has started.");
        if ("COMPLETED".equals(record.status))
            throw new IllegalStateException(
                    "Voters cannot be removed because the election has already ended.");
        if (voterDAO.hasVoted(electionId, voterId))
            throw new IllegalStateException(
                    "This voter has already voted and cannot be removed.");
        voterDAO.removeVoterFromElection(electionId, voterId);
    }

    public synchronized void removeCandidateFromElection(int candidateId) throws SQLException {
        requireAdmin();
        ElectionDAO.ElectionRecord record = requireElection();
        if ("ACTIVE".equals(record.status))
            throw new IllegalStateException(
                    "Candidates cannot be removed after the election has started.");
        if ("COMPLETED".equals(record.status))
            throw new IllegalStateException(
                    "Candidates cannot be removed because the election has already ended.");
        if (candidateDAO.candidateHasVotes(electionId, candidateId))
            throw new IllegalStateException(
                    "This candidate has already received votes and cannot be removed.");
        candidateDAO.removeCandidateFromElection(electionId, candidateId);
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

    // Whether the currently logged-in voter has already voted in this election.
    // Lets the voter see their own status without exposing anyone else's.
    public synchronized boolean hasCurrentVoterVoted() throws SQLException {
        if (!session.isLoggedIn())
            throw new IllegalStateException("No voter is logged in. Please log in first.");
        return voterDAO.hasVoted(electionId, session.getVoterId());
    }

    // Every election, newest first — used by the admin's Select Election list
    public static List<ElectionDAO.ElectionRecord> getAllElections() throws SQLException {
        requireAdmin();
        return electionDAO.getAllElections();
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
