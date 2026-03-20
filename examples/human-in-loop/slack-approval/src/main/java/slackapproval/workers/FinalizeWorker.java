package slackapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for sa_finalize — processes the approval decision after the WAIT task
 * receives the Slack response.
 *
 * Reads the approval decision from input and returns { done: true, decision: "..." }.
 */
public class FinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sa_finalize";
    }

    @Override
    public TaskResult execute(Task task) {
        String decision = "unknown";
        Object decisionInput = task.getInputData().get("decision");
        if (decisionInput instanceof String && !((String) decisionInput).isEmpty()) {
            decision = (String) decisionInput;
        }

        System.out.println("  [sa_finalize] Finalizing with decision: " + decision);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("done", true);
        result.getOutputData().put("decision", decision);
        return result;
    }
}
