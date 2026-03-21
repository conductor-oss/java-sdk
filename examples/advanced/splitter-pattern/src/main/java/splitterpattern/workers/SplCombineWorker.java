package splitterpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SplCombineWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "spl_combine";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [combine] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("combinedResult", java.util.Map.of("totalAmount", 1809.96, "fulfilledCount", 3));
        result.getOutputData().put("allPartsProcessed", true);
        return result;
    }
}