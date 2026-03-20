package taskrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrtSelectPoolWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "trt_select_pool";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [select] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("selectedPool", "gpu-pool-us");
        result.getOutputData().put("poolLoad", 0.6);
        return result;
    }
}