package salvagerecovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssessDamageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "slv_assess_damage";
    }

    @Override
    public TaskResult execute(Task task) {

        String vehicleId = (String) task.getInputData().get("vehicleId");
        System.out.printf("  [assess] Vehicle %s total loss — salvage value: $4,200%n", vehicleId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("totalLoss", true);
        result.getOutputData().put("salvageValue", 4200);
        return result;
    }
}
