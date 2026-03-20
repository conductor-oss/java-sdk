package backpressure.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class BkpThrottleWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bkp_throttle";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [throttle] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("action", "throttled");
        result.getOutputData().put("throttlePercent", task.getInputData().getOrDefault("throttlePercent", 40));
        return result;
    }
}