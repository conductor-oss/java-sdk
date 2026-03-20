package helpdeskrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class Tier1Worker implements Worker {
    @Override public String getTaskDefName() { return "hdr_tier1"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [tier1] Basic support handling issue for " + task.getInputData().get("customerId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "T1 Support - Alex");
        result.getOutputData().put("resolution", "Knowledge base article provided");
        result.getOutputData().put("avgResponseMin", 15);
        return result;
    }
}
