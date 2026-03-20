package helpdeskrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class Tier3Worker implements Worker {
    @Override public String getTaskDefName() { return "hdr_tier3"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [tier3] Senior engineering handling issue for " + task.getInputData().get("customerId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "T3 Senior SRE - Nate");
        result.getOutputData().put("resolution", "Root cause identified and hotfix deployed");
        result.getOutputData().put("avgResponseMin", 120);
        return result;
    }
}
