package sequentialtasks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Extract phase of the ETL pipeline.
 * Takes a data source name and returns hardcoded raw records.
 */
public class ExtractWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "seq_extract";
    }

    @Override
    public TaskResult execute(Task task) {
        String source = (String) task.getInputData().get("source");
        if (source == null || source.isBlank()) {
            source = "default";
        }

        System.out.println("  [seq_extract] Extracting data from source: " + source);

        List<Map<String, Object>> data = List.of(
                Map.of("id", 1, "name", "Alice", "score", 85),
                Map.of("id", 2, "name", "Bob", "score", 92),
                Map.of("id", 3, "name", "Carol", "score", 78)
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rawData", data);
        result.getOutputData().put("source", source);
        result.getOutputData().put("recordCount", data.size());
        return result;
    }
}
