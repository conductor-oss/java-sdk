package contractlifecycle.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Renews an active contract.
 */
public class RenewWorker implements Worker {
    @Override public String getTaskDefName() { return "clf_renew"; }

    @Override public TaskResult execute(Task task) {
        Object executedObj = task.getInputData().get("executed");
        boolean wasExecuted = Boolean.TRUE.equals(executedObj);

        boolean renewed = wasExecuted;
        Instant newExpiration = Instant.now().plus(365, ChronoUnit.DAYS);

        System.out.println("  [renew] Contract " + (renewed ? "renewed" : "not eligible for renewal"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("contractState", renewed ? "RENEWED" : "EXPIRED");
        result.getOutputData().put("renewed", renewed);
        result.getOutputData().put("newExpirationDate", renewed ? newExpiration.toString() : null);
        return result;
    }
}
