package contractlifecycle.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Executes an approved contract.
 */
public class ExecuteWorker implements Worker {
    @Override public String getTaskDefName() { return "clf_execute"; }

    @Override public TaskResult execute(Task task) {
        Object approvedObj = task.getInputData().get("approved");
        boolean approved = Boolean.TRUE.equals(approvedObj);

        Instant effectiveDate = Instant.now();
        Instant expirationDate = effectiveDate.plus(365, ChronoUnit.DAYS);

        System.out.println("  [execute] Contract " + (approved ? "executed" : "not executed"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("contractState", approved ? "ACTIVE" : "CANCELLED");
        result.getOutputData().put("executed", approved);
        result.getOutputData().put("effectiveDate", effectiveDate.toString());
        result.getOutputData().put("expirationDate", expirationDate.toString());
        return result;
    }
}
