package escalationchain.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Finalizes the escalation decision. Records outcome with audit trail.
 */
public class EscFinalizeWorker implements Worker {
    @Override public String getTaskDefName() { return "esc_finalize"; }

    @Override public TaskResult execute(Task task) {
        Object rawDecision = task.getInputData().get("decision");
        Object rawLevel = task.getInputData().get("level");
        String decision = (rawDecision instanceof String) ? (String) rawDecision : "unknown";
        String level = (rawLevel instanceof String) ? (String) rawLevel : "unknown";

        boolean approved = "approved".equalsIgnoreCase(decision) || "approve".equalsIgnoreCase(decision);

        System.out.println("  [esc_finalize] " + decision + " at " + level);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("done", true);
        result.getOutputData().put("decision", decision);
        result.getOutputData().put("resolvedAt", level);
        result.getOutputData().put("approved", approved);
        result.getOutputData().put("finalizedAt", Instant.now().toString());
        return result;
    }
}
