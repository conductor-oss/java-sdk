package workflowcomposition.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class WcpSubBStep1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wcp_sub_b_step1";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerId = (String) task.getInputData().getOrDefault("customerId", "unknown");
        System.out.println("  [sub-B/step1] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("profile", Map.of("tier", "gold", "since", "2020-01-15"));
        return result;
    }
}