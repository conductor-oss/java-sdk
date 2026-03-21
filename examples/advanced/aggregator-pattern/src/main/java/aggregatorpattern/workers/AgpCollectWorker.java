package aggregatorpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AgpCollectWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agp_collect";
    }

    @Override
    public TaskResult execute(Task task) {
        Object msgs = task.getInputData().getOrDefault("messages", java.util.List.of());
        int count = msgs instanceof java.util.List ? ((java.util.List<?>) msgs).size() : 0;
        System.out.println("  [collect] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("collectedMessages", msgs);
        result.getOutputData().put("collectedCount", count);
        return result;
    }
}