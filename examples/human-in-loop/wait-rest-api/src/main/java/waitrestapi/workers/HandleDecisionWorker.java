package waitrestapi.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for wapi_handle_decision — handles the outcome of the approval decision.
 *
 * Reads the "decision" field from input and returns { result: "handled-{decision}" }.
 */
public class HandleDecisionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wapi_handle_decision";
    }

    @Override
    public TaskResult execute(Task task) {
        String decision = "unknown";
        Object decisionInput = task.getInputData().get("decision");
        if (decisionInput instanceof String) {
            decision = (String) decisionInput;
        }

        System.out.println("  [wapi_handle_decision] Handling decision: " + decision);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "handled-" + decision);

        return result;
    }
}
