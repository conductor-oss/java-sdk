package dataexportrequest.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class DeliverExportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "der_deliver";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        System.out.println("  [deliver] Export download link sent to user " + userId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("delivered", true);
        result.getOutputData().put("expiresAt", Instant.now().plus(7, ChronoUnit.DAYS).toString());
        return result;
    }
}
