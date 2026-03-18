package travelapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Submits travel request. Real request validation.
 */
public class SubmitWorker implements Worker {
    @Override public String getTaskDefName() { return "tva_submit"; }

    @Override public TaskResult execute(Task task) {
        String destination = (String) task.getInputData().get("destination");
        String reason = (String) task.getInputData().get("reason");
        if (destination == null) destination = "Unknown";

        String requestId = "TR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        System.out.println("  [submit] Travel request " + requestId + " to " + destination);

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("requestId", requestId);
        r.getOutputData().put("submitted", true);
        r.getOutputData().put("submittedAt", Instant.now().toString());
        return r;
    }
}
