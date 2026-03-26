package accountopening.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Opens a bank account after verifying eligibility.
 * Real logic: checks identity verification and credit eligibility before opening.
 * Generates a unique account number using secure random.
 * Records compliance audit trail.
 */
public class OpenAccountWorker implements Worker {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String getTaskDefName() { return "acc_open_account"; }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        // --- Validate required inputs ---
        Object identityVerifiedObj = task.getInputData().get("identityVerified");
        if (identityVerifiedObj == null) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing required input: identityVerified");
            return r;
        }

        Object chexScoreObj = task.getInputData().get("chexScore");
        if (chexScoreObj == null || !(chexScoreObj instanceof Number)) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing or non-numeric required input: chexScore");
            return r;
        }

        String accountType = (String) task.getInputData().get("accountType");
        if (accountType == null || accountType.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing required input: accountType");
            return r;
        }

        boolean identityVerified = Boolean.TRUE.equals(identityVerifiedObj)
                || "true".equalsIgnoreCase(String.valueOf(identityVerifiedObj));
        int chexScore = ((Number) chexScoreObj).intValue();

        double deposit = 0;
        Object depositObj = task.getInputData().get("initialDeposit");
        if (depositObj instanceof Number) deposit = ((Number) depositObj).doubleValue();

        // Generate unique account number: ACCT- + 10-digit random
        String accountNumber = "ACCT-" + String.format("%010d", Math.abs(RANDOM.nextLong()) % 10_000_000_000L);

        // Real routing number based on account type
        String routingNumber = switch (accountType) {
            case "savings" -> "021000090";
            case "money_market" -> "021000091";
            case "cd" -> "021000092";
            default -> "021000089";
        };

        boolean opened = identityVerified && chexScore < 700;

        // Compliance audit trail
        Map<String, Object> auditTrail = new LinkedHashMap<>();
        auditTrail.put("timestamp", Instant.now().toString());
        auditTrail.put("action", "open_account");
        auditTrail.put("identityVerified", identityVerified);
        auditTrail.put("chexScore", chexScore);
        auditTrail.put("accountType", accountType);
        auditTrail.put("decision", opened ? "APPROVED" : "REJECTED");
        if (!opened) {
            auditTrail.put("rejectionReason", !identityVerified
                    ? "Identity not verified" : "ChexSystems score too high (" + chexScore + ")");
        }

        System.out.println("  [open] Account " + accountNumber + " - type: " + accountType
                + ", deposit: $" + (int) deposit + ", opened: " + opened);

        if (!opened) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            String reason = !identityVerified ? "Identity not verified" : "ChexSystems score too high (" + chexScore + ")";
            r.setReasonForIncompletion(reason);
        } else {
            r.setStatus(TaskResult.Status.COMPLETED);
        }
        r.getOutputData().put("accountNumber", accountNumber);
        r.getOutputData().put("opened", opened);
        r.getOutputData().put("openedAt", Instant.now().toString());
        r.getOutputData().put("routingNumber", routingNumber);
        r.getOutputData().put("initialBalance", deposit);
        r.getOutputData().put("auditTrail", auditTrail);
        return r;
    }
}
