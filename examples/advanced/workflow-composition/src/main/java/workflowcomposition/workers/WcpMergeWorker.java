package workflowcomposition.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WcpMergeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wcp_merge";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderResult = (String) task.getInputData().getOrDefault("orderResult", "unknown");
        String customerResult = (String) task.getInputData().getOrDefault("customerResult", "unknown");
        System.out.println("  [merge] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("composedResult", "order_with_customer_context");
        result.getOutputData().put("orderProcessed", true);
        result.getOutputData().put("customerEnriched", true);
        return result;
    }
}