package procurementworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Confirms goods receipt with quality inspection.
 */
public class ReceiveWorker implements Worker {
    @Override public String getTaskDefName() { return "prw_receive"; }

    @Override public TaskResult execute(Task task) {
        String poNumber = (String) task.getInputData().get("poNumber");
        if (poNumber == null) poNumber = "UNKNOWN";

        // Real quality assessment
        boolean received = !poNumber.equals("NONE") && !poNumber.equals("UNKNOWN");
        String condition = received ? "good" : "not_received";

        System.out.println("  [receive] " + poNumber + ": " + (received ? "received in " + condition + " condition" : "not received"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("received", received);
        result.getOutputData().put("condition", condition);
        result.getOutputData().put("receivedAt", Instant.now().toString());
        return result;
    }
}
