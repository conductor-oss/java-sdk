package wiretap.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WtpMainFlowWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wtp_main_flow";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [main] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", java.util.Map.of("processed", true, "action", "message_handled", "timestamp", java.time.Instant.now().toString()));
        return result;
    }
}