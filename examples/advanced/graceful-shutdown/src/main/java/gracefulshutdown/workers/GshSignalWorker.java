package gracefulshutdown.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GshSignalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gsh_signal";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [signal] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("signalSent", true);
        result.getOutputData().put("timestamp", java.time.Instant.now().toString());
        return result;
    }
}