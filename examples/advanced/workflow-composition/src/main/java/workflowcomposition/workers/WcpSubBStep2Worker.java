package workflowcomposition.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WcpSubBStep2Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wcp_sub_b_step2";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [sub-B/step2] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "customer_enriched");
        result.getOutputData().put("discount", "10%");
        return result;
    }
}