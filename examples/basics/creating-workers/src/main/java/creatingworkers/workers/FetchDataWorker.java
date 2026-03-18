package creatingworkers.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that performs an async data fetch operation.
 *
 * Takes a source name and returns a list of records.
 * In a real application, this would call an external API or database.
 * Demonstrates how workers can encapsulate I/O operations while Conductor
 * manages retries and timeouts.
 */
public class FetchDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fetch_data";
    }

    @Override
    public TaskResult execute(Task task) {
        String source = (String) task.getInputData().get("source");
        if (source == null || source.isBlank()) {
            source = "default";
        }

        System.out.println("  [fetch_data] Fetching records from source: \"" + source + "\"");

        // Brief processing delay to demonstrate async pattern
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Generate deterministic records based on source name
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "value", source + "-record-1", "score", 85),
                Map.of("id", 2, "value", source + "-record-2", "score", 92),
                Map.of("id", 3, "value", source + "-record-3", "score", 78)
        );

        System.out.println("  [fetch_data] Fetched " + records.size() + " records.");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", records);
        result.getOutputData().put("source", source);
        result.getOutputData().put("recordCount", records.size());
        return result;
    }
}
