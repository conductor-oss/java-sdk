package calendarintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class FetchEventsWorker implements Worker {
    @Override public String getTaskDefName() { return "cal_fetch_events"; }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [fetch] Fetching events from calendar " + task.getInputData().get("calendarId"));
        result.getOutputData().put("events", List.of(
            Map.of("id", "evt-1", "title", "Sprint Planning", "start", "2026-03-09T10:00:00Z"),
            Map.of("id", "evt-2", "title", "Standup", "start", "2026-03-09T09:00:00Z")));
        result.getOutputData().put("eventCount", 2);
        return result;
    }
}
