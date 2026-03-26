package helpdeskrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class Tier2Worker implements Worker {
    @Override public String getTaskDefName() { return "hdr_tier2"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [tier2] Technical support handling issue for " + task.getInputData().get("customerId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "T2 Engineer - Priya");
        result.getOutputData().put("resolution", "API configuration corrected");
        result.getOutputData().put("avgResponseMin", 60);
        return result;
    }
}
