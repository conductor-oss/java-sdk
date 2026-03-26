package eventwindowing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Collects incoming events into a fixed time window. Passes through all events
 * that fall within the configured window, tagging them with a window identifier.
 */
public class CollectWindowWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ew_collect_window";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> events =
                (List<Map<String, Object>>) task.getInputData().get("events");
        if (events == null) {
            events = List.of();
        }

        Object windowSizeRaw = task.getInputData().get("windowSizeMs");
        int windowSizeMs = 5000;
        if (windowSizeRaw instanceof Number) {
            windowSizeMs = ((Number) windowSizeRaw).intValue();
        }

        System.out.println("  [ew_collect_window] Collecting " + events.size()
                + " events into window of " + windowSizeMs + "ms");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("windowEvents", events);
        result.getOutputData().put("windowId", "win_fixed_001");
        result.getOutputData().put("windowSizeMs", windowSizeMs);
        return result;
    }
}
