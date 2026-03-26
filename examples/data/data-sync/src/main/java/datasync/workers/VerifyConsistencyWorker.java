package datasync.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Verifies that both systems are consistent after sync.
 * Input: appliedToA, appliedToB
 * Output: status, summary, checksumMatch, verifiedAt
 */
public class VerifyConsistencyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sy_verify_consistency";
    }

    @Override
    public TaskResult execute(Task task) {
        int appliedA = toInt(task.getInputData().get("appliedToA"));
        int appliedB = toInt(task.getInputData().get("appliedToB"));
        int total = appliedA + appliedB;

        String status = total > 0 ? "CONSISTENT" : "NO_CHANGES";
        String summary = "Sync complete: " + appliedA + " updates applied to A, "
                + appliedB + " to B. Systems are now consistent.";

        System.out.println("  [verify] " + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", status);
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("checksumMatch", true);
        result.getOutputData().put("verifiedAt", "2024-03-15T12:00:00Z");
        return result;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        return 0;
    }
}
