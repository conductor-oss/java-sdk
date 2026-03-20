package workflowcomposition.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WcpSubAStep1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wcp_sub_a_step1";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().getOrDefault("orderId", "unknown");
        System.out.println("  [sub-A/step1] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validated", true);
        result.getOutputData().put("orderId", orderId);
        return result;
    }
}