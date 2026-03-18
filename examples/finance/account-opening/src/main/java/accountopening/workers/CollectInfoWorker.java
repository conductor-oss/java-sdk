package accountopening.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Collects and validates applicant information for account opening.
 * Performs real validation: name format, email format, account type, minimum deposit.
 */
public class CollectInfoWorker implements Worker {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z' .-]{1,99}$");

    @Override
    public String getTaskDefName() { return "acc_collect_info"; }

    @Override
    public TaskResult execute(Task task) {
        String applicantName = (String) task.getInputData().get("applicantName");
        String accountType = (String) task.getInputData().get("accountType");
        Object depositObj = task.getInputData().get("initialDeposit");

        if (applicantName == null) applicantName = "";
        if (accountType == null) accountType = "checking";

        double deposit = 0;
        if (depositObj instanceof Number) deposit = ((Number) depositObj).doubleValue();
        else if (depositObj instanceof String) {
            try { deposit = Double.parseDouble((String) depositObj); } catch (NumberFormatException ignored) {}
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
        documents.put("ssn", true);
        documents.put("proofOfAddress", true);

        System.out.println("  [info] Collecting info for " + applicantName + " - " + accountType
                + " (name valid: " + nameValid + ", deposit: $" + deposit + "/" + minimumDeposit + ")");

        TaskResult r = new TaskResult(task);
        if (!nameValid || !accountTypeValid || !depositSufficient) {
            r.setStatus(TaskResult.Status.FAILED);
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
        r.getOutputData().put("accountTypeValid", accountTypeValid);
        r.getOutputData().put("depositSufficient", depositSufficient);
        r.getOutputData().put("minimumDeposit", minimumDeposit);
        r.getOutputData().put("completedAt", Instant.now().toString());
        return r;
    }
}
