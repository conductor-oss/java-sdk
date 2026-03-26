package clickstreamanalytics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ingests click events from a tracking source.
 * Input: clickData
 * Output: events, clickCount
 */
public class IngestClicksWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ck_ingest_clicks";
    }

    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> events = List.of(
                Map.of("userId", "U1", "page", "/home", "ts", 1000, "action", "view"),
                Map.of("userId", "U1", "page", "/products", "ts", 1030, "action", "view"),
                Map.of("userId", "U1", "page", "/products/widget", "ts", 1045, "action", "click"),
                Map.of("userId", "U1", "page", "/cart", "ts", 1060, "action", "add"),
                Map.of("userId", "U1", "page", "/checkout", "ts", 1090, "action", "purchase"),
                Map.of("userId", "U2", "page", "/home", "ts", 1010, "action", "view"),
                Map.of("userId", "U2", "page", "/products", "ts", 1025, "action", "view"),
                Map.of("userId", "U2", "page", "/home", "ts", 1080, "action", "view"),
                Map.of("userId", "U3", "page", "/home", "ts", 1005, "action", "view"),
                Map.of("userId", "U3", "page", "/about", "ts", 1015, "action", "view")
        );

        Set<String> users = events.stream().map(e -> (String) e.get("userId")).collect(Collectors.toSet());
        System.out.println("  [ingest] Ingested " + events.size() + " click events from " + users.size() + " users");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("events", events);
        result.getOutputData().put("clickCount", events.size());
        return result;
    }
}
