package accountopening.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Opens a bank account after verifying eligibility.
 * Real logic: checks identity verification and credit eligibility before opening.
 * Generates a unique account number using secure random.
 */
public class OpenAccountWorker implements Worker {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String getTaskDefName() { return "acc_open_account"; }

    @Override
    public TaskResult execute(Task task) {
        Object identityVerifiedObj = task.getInputData().get("identityVerified");
        Object chexScoreObj = task.getInputData().get("chexScore");
        String accountType = (String) task.getInputData().get("accountType");
        Object depositObj = task.getInputData().get("initialDeposit");

        if (accountType == null) accountType = "checking";

        boolean identityVerified = Boolean.TRUE.equals(identityVerifiedObj)
                || "true".equalsIgnoreCase(String.valueOf(identityVerifiedObj));

        int chexScore = 0;
        if (chexScoreObj instanceof Number) chexScore = ((Number) chexScoreObj).intValue();

        double deposit = 0;
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

        System.out.println("  [open] Account " + accountNumber + " - type: " + accountType
                + ", deposit: $" + (int) deposit + ", opened: " + opened);

        TaskResult r = new TaskResult(task);
        if (!opened) {
            r.setStatus(TaskResult.Status.FAILED);
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
        return r;
    }
}
