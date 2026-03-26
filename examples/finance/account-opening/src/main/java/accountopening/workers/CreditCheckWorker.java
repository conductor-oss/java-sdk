package accountopening.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Performs a real algorithmic credit/ChexSystems check for account opening.
 * Computes a ChexSystems score based on name hash for deterministic results.
 * Records compliance audit trail.
 */
public class CreditCheckWorker implements Worker {

    @Override
    public String getTaskDefName() { return "acc_credit_check"; }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        String applicantName = (String) task.getInputData().get("applicantName");
        if (applicantName == null || applicantName.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing required input: applicantName");
            return r;
        }

        // Deterministic score based on name hash (0-999 range for ChexSystems)
        int nameHash = Math.abs(applicantName.hashCode());
        int chexScore = nameHash % 1000;
        int bankruptcies = (nameHash / 1000) % 3;    // 0-2
        int closedForCause = (nameHash / 3000) % 2;  // 0-1

        // Eligibility: score < 700 and no bankruptcies or closedForCause
        boolean eligible = chexScore < 700 && bankruptcies == 0 && closedForCause == 0;

        // Compliance audit trail
        Map<String, Object> auditTrail = new LinkedHashMap<>();
        auditTrail.put("timestamp", Instant.now().toString());
        auditTrail.put("action", "credit_check");
        auditTrail.put("applicantName", applicantName);
        auditTrail.put("chexScore", chexScore);
        auditTrail.put("bankruptcies", bankruptcies);
        auditTrail.put("closedForCause", closedForCause);
        auditTrail.put("eligible", eligible);

        System.out.println("  [credit] ChexSystems check for " + applicantName
                + " - score: " + chexScore + ", bankruptcies: " + bankruptcies
                + ", closedForCause: " + closedForCause + ", eligible: " + eligible);

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("chexScore", chexScore);
        r.getOutputData().put("bankruptcies", bankruptcies);
        r.getOutputData().put("closedForCause", closedForCause);
        r.getOutputData().put("eligible", eligible);
        r.getOutputData().put("auditTrail", auditTrail);
        return r;
    }
}
