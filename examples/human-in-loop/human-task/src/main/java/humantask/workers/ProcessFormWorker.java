package humantask.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for ht_process_form — processes the human review decision.
 *
 * Reads the "approved" flag from input and returns either
 * "application-approved" or "application-rejected" as the decision.
 */
public class ProcessFormWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ht_process_form";
    }

    @Override
    public TaskResult execute(Task task) {
        Object approvedInput = task.getInputData().get("approved");
        boolean approved = Boolean.TRUE.equals(approvedInput);

        String decision = approved ? "application-approved" : "application-rejected";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("decision", decision);
        return result;
    }
}
