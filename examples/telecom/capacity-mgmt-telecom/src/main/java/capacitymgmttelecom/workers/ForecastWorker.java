package capacitymgmttelecom.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ForecastWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cmt_forecast";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [forecast] Capacity threshold reached in 3 months");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("forecast", java.util.Map.of("monthsToThreshold", 3, "projectedPeak", 95));
        return result;
    }
}
