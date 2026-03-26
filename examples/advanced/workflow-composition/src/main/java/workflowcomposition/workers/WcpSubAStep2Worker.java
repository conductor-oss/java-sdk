package workflowcomposition.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WcpSubAStep2Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wcp_sub_a_step2";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [sub-A/step2] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "order_processed");
        result.getOutputData().put("total", 299.99);
        return result;
    }
}