package aggregatorpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AgpForwardWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agp_forward";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [forward] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("forwarded", true);
        result.getOutputData().put("destination", "downstream_processor");
        result.getOutputData().put("forwardedAt", java.time.Instant.now().toString());
        return result;
    }
}