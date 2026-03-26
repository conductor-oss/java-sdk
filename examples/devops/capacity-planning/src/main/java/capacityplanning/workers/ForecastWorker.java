package capacityplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ForecastWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cp_forecast";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [forecast] Capacity exceeded in 21 days");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("daysUntilCapacity", 21);
        result.addOutputData("confidence", 0.89);
        return result;
    }
}
