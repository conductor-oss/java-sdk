package roamingmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rmg_rate";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [rate] Roaming usage rated: $12.50 subscriber / $8.75 inter-carrier");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("charges", 12.5);
        result.getOutputData().put("interCarrierAmount", 8.75);
        return result;
    }
}
