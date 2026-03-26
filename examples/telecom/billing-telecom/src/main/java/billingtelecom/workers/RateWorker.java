package billingtelecom.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "btl_rate";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [rate] Usage rated — voice: $16.00, data: $31.00, sms: $7.50");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ratedCharges", java.util.Map.of("voice", 16.0, "data", 31.0, "sms", 7.5));
        return result;
    }
}
