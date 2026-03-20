package webinarregistration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class RemindWorker implements Worker {
    @Override public String getTaskDefName() { return "wbr_remind"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [remind] Reminder sent: 24h and 1h before event");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reminders", List.of(
                Map.of("type", "24h", "sent", true),
                Map.of("type", "1h", "sent", true)));
        return result;
    }
}
