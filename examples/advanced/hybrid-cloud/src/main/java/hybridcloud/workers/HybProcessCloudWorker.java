package hybridcloud.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class HybProcessCloudWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hyb_process_cloud";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [cloud] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processedAt", "aws-us-east-1");
        result.getOutputData().put("scaledInstances", 4);
        return result;
    }
}