package billingtelecom.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectUsageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "btl_collect_usage";
    }

    @Override
    public TaskResult execute(Task task) {

        String customerId = (String) task.getInputData().get("customerId");
        System.out.printf("  [usage] Collected usage for customer %s%n", customerId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("usageRecords", java.util.Map.of("voice", 320, "data", 15.5, "sms", 150));
        return result;
    }
}
