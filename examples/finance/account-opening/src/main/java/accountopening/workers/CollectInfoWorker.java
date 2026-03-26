package accountopening.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Collects and validates applicant information for account opening.
 * Performs real validation: name format, SSN format, date of birth, email format, account type, minimum deposit.
 * Records a compliance audit trail in output.
 */
public class CollectInfoWorker implements Worker {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z' .-]{1,99}$");
    /** SSN format: XXX-XX-XXXX */
    private static final Pattern SSN_PATTERN = Pattern.compile("^\\d{3}-\\d{2}-\\d{4}$");

    @Override
    public String getTaskDefName() { return "acc_collect_info"; }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        // --- Validate required inputs ---
        String applicantName = (String) task.getInputData().get("applicantName");
        if (applicantName == null || applicantName.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing required input: applicantName");
            return r;
        }

        String accountType = (String) task.getInputData().get("accountType");
        if (accountType == null || accountType.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing required input: accountType");
            return r;
        }

        Object depositObj = task.getInputData().get("initialDeposit");
        if (depositObj == null || !(depositObj instanceof Number)) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing or non-numeric required input: initialDeposit");
            return r;
        }
        double deposit = ((Number) depositObj).doubleValue();

        // --- SSN validation ---
        String ssn = (String) task.getInputData().get("ssn");
        boolean ssnValid = ssn != null && SSN_PATTERN.matcher(ssn).matches();
        // SSN must not be all-zeros in any group
        if (ssnValid) {
            String[] parts = ssn.split("-");
            ssnValid = !"000".equals(parts[0]) && !"00".equals(parts[1]) && !"0000".equals(parts[2]);
        }

        // --- Date of birth validation ---
        String dob = (String) task.getInputData().get("dateOfBirth");
        boolean dobValid = false;
        int age = -1;
        if (dob != null) {
            try {
                LocalDate birthDate = LocalDate.parse(dob, DateTimeFormatter.ISO_LOCAL_DATE);
                LocalDate today = LocalDate.now();
                if (birthDate.isBefore(today) && birthDate.isAfter(today.minusYears(120))) {
                    age = Period.between(birthDate, today).getYears();
                    dobValid = age >= 18; // Must be at least 18
                }
            } catch (DateTimeParseException ignored) {
                // dobValid remains false
            }
        }

        // Real validation
        boolean nameValid = NAME_PATTERN.matcher(applicantName).matches();
        boolean accountTypeValid = "checking".equals(accountType) || "savings".equals(accountType)
                || "money_market".equals(accountType) || "cd".equals(accountType);

        double minimumDeposit = switch (accountType) {
            case "savings" -> 100;
            case "money_market" -> 2500;
            case "cd" -> 1000;
            default -> 25;
        };
        boolean depositSufficient = deposit >= minimumDeposit;

        Map<String, Object> documents = new LinkedHashMap<>();
        documents.put("driversLicense", nameValid);
        documents.put("ssn", ssnValid);
        documents.put("proofOfAddress", true);

        System.out.println("  [info] Collecting info for " + applicantName + " - " + accountType
                + " (name valid: " + nameValid + ", ssn valid: " + ssnValid
                + ", dob valid: " + dobValid + ", deposit: $" + deposit + "/" + minimumDeposit + ")");

        // Build compliance audit trail
        Map<String, Object> auditTrail = new LinkedHashMap<>();
        auditTrail.put("timestamp", Instant.now().toString());
        auditTrail.put("action", "collect_applicant_info");
        auditTrail.put("applicantName", applicantName);
        auditTrail.put("nameValidated", nameValid);
        auditTrail.put("ssnFormatValidated", ssnValid);
        auditTrail.put("dobValidated", dobValid);
        auditTrail.put("accountTypeValidated", accountTypeValid);
        auditTrail.put("depositSufficient", depositSufficient);
        if (age >= 0) auditTrail.put("applicantAge", age);

        if (!nameValid || !accountTypeValid || !depositSufficient) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            StringBuilder reason = new StringBuilder();
            if (!nameValid) reason.append("Invalid name format. ");
            if (!accountTypeValid) reason.append("Invalid account type '").append(accountType).append("'. ");
            if (!depositSufficient) reason.append("Minimum deposit for ").append(accountType)
                    .append(" is $").append((int) minimumDeposit).append(", got $").append((int) deposit).append(". ");
            r.setReasonForIncompletion(reason.toString().trim());
        } else {
            r.setStatus(TaskResult.Status.COMPLETED);
        }
        r.getOutputData().put("documents", documents);
        r.getOutputData().put("nameValid", nameValid);
        r.getOutputData().put("ssnValid", ssnValid);
        r.getOutputData().put("dobValid", dobValid);
        r.getOutputData().put("accountTypeValid", accountTypeValid);
        r.getOutputData().put("depositSufficient", depositSufficient);
        r.getOutputData().put("minimumDeposit", minimumDeposit);
        r.getOutputData().put("auditTrail", auditTrail);
        r.getOutputData().put("completedAt", Instant.now().toString());
        return r;
    }
}
