package waittimeoutescalation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for wte_escalate — handles escalation when the WAIT task times out.
 *
 * Returns { escalated: true, escalatedTo: "manager@company.com" }.
 */
public class WteEscalateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wte_escalate";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [wte_escalate] Escalating due to timeout...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("escalated", true);
        result.getOutputData().put("escalatedTo", "manager@company.com");

        return result;
    }
}
