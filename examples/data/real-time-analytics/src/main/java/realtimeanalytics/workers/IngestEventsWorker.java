package realtimeanalytics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Ingests a batch of events for real-time analytics processing.
 * Input: eventBatch
 * Output: events (list), eventCount
 */
public class IngestEventsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ry_ingest_events";
    }

    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> events = List.of(
                Map.of("eventId", "E001", "type", "page_view", "userId", "U1", "page", "/home", "ts", 1700500000, "latency", 120),
                Map.of("eventId", "E002", "type", "page_view", "userId", "U2", "page", "/products", "ts", 1700500001, "latency", 250),
                Map.of("eventId", "E003", "type", "api_call", "userId", "U1", "endpoint", "/api/search", "ts", 1700500002, "latency", 580),
                Map.of("eventId", "E004", "type", "error", "userId", "U3", "error", "500_internal", "ts", 1700500003, "latency", 0),
                Map.of("eventId", "E005", "type", "page_view", "userId", "U4", "page", "/checkout", "ts", 1700500004, "latency", 180),
                Map.of("eventId", "E006", "type", "api_call", "userId", "U2", "endpoint", "/api/cart", "ts", 1700500005, "latency", 95),
                Map.of("eventId", "E007", "type", "error", "userId", "U5", "error", "timeout", "ts", 1700500006, "latency", 30000),
                Map.of("eventId", "E008", "type", "page_view", "userId", "U6", "page", "/home", "ts", 1700500007, "latency", 140)
        );

        Set<String> types = new LinkedHashSet<>();
        Set<String> users = new LinkedHashSet<>();
        for (Map<String, Object> e : events) {
            types.add((String) e.get("type"));
            users.add((String) e.get("userId"));
        }

        System.out.println("  [ingest] Ingested " + events.size() + " events ("
                + types.size() + " types, " + users.size() + " users)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("events", events);
        result.getOutputData().put("eventCount", events.size());
        return result;
    }
}
