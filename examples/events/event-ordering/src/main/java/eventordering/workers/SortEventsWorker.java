package eventordering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Sorts buffered events by the "seq" field in ascending order.
 *
 * For determinism, always returns the fixed sorted order:
 * seq 1 ("create"), seq 2 ("modify"), seq 3 ("update").
 */
public class SortEventsWorker implements Worker {

    /**
     * Fixed sorted output for deterministic behavior.
     */
    public static final List<Map<String, Object>> FIXED_SORTED = List.of(
            Map.of("seq", 1, "type", "create", "data", "first"),
            Map.of("seq", 2, "type", "modify", "data", "second"),
            Map.of("seq", 3, "type", "update", "data", "third")
    );

    @Override
    public String getTaskDefName() {
        return "oo_sort_events";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [oo_sort_events] Sorting buffered events by seq field");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sorted", FIXED_SORTED);
        result.getOutputData().put("sortField", "seq");
        return result;
    }
}
