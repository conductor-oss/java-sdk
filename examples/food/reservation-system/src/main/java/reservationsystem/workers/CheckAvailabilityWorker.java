package reservationsystem.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class CheckAvailabilityWorker implements Worker {
    @Override public String getTaskDefName() { return "rsv_check_availability"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [avail] Checking " + task.getInputData().get("date") + " at " + task.getInputData().get("time") + " for " + task.getInputData().get("partySize"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("slot", Map.of("available", true, "tableId", "T-8", "section", "indoor"));
        return result;
    }
}
