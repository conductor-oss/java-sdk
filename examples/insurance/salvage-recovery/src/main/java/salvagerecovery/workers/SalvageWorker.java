package salvagerecovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SalvageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "slv_salvage";
    }

    @Override
    public TaskResult execute(Task task) {

        String vehicleId = (String) task.getInputData().get("vehicleId");
        System.out.printf("  [salvage] Vehicle %s picked up%n", vehicleId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reservePrice", 4200);
        result.getOutputData().put("lotNumber", "LOT-701");
        return result;
    }
}
