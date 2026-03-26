package eventmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class PlanWorker implements Worker {
    @Override public String getTaskDefName() { return "evt_plan"; }

    @Override
    public TaskResult execute(Task task) {
        String eventId = "EVT-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        System.out.println("  [plan] Event \"" + task.getInputData().get("eventName") + "\" planned for " + task.getInputData().get("date") + " -> " + eventId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("eventId", eventId);
        result.getOutputData().put("venue", "Convention Center Hall A");
        result.getOutputData().put("logistics", Map.of("catering", true, "av", true, "parking", true));
        return result;
    }
}
