package eventbatching.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Collects incoming events and returns them along with a total count.
 *
 * Input:  {events: [{id, type}, ...]}
 * Output: {events: [...], totalCount: events.size()}
 */
public class CollectEventsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eb_collect_events";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> events =
                (List<Map<String, Object>>) task.getInputData().get("events");

        int totalCount = (events != null) ? events.size() : 0;

        System.out.println("  [eb_collect_events] Collected " + totalCount + " events");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("events", events != null ? events : List.of());
        result.getOutputData().put("totalCount", totalCount);
        return result;
    }
}
