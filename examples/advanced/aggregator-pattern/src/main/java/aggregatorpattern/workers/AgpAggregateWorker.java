package aggregatorpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AgpAggregateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agp_aggregate";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [aggregate] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("aggregatedResult", java.util.Map.of("messageCount", 4, "totalAmount", 885.00, "aggregatedAt", java.time.Instant.now().toString()));
        return result;
    }
}