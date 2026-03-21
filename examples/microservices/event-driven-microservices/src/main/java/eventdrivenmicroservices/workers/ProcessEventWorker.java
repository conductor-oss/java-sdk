package eventdrivenmicroservices.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class ProcessEventWorker implements Worker {
    @Override public String getTaskDefName() { return "edm_process_event"; }
    @Override public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().getOrDefault("eventType", "UNKNOWN");
        String eventId = (String) task.getInputData().getOrDefault("eventId", "unknown");
        System.out.println("  [process] Handling " + eventType + ": " + eventId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("result", "order_updated");
        r.getOutputData().put("subscribers", List.of("billing", "shipping", "analytics"));
        return r;
    }
}
