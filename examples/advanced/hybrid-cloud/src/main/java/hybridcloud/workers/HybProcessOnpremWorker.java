package hybridcloud.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class HybProcessOnpremWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hyb_process_onprem";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [on-prem] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processedAt", "datacenter-1");
        result.getOutputData().put("encrypted", true);
        return result;
    }
}