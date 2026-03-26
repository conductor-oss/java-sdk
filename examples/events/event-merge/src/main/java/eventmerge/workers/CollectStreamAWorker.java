package eventmerge.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Collects events from stream A (API source).
 * Input: source
 * Output: events (list of event maps), count
 */
public class CollectStreamAWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mg_collect_stream_a";
    }

    @Override
    public TaskResult execute(Task task) {
        String source = (String) task.getInputData().get("source");
        if (source == null) {
            source = "api";
        }

        System.out.println("  [mg_collect_stream_a] Collecting from source: " + source);

        List<Map<String, String>> events = List.of(
                Map.of("id", "a1", "source", "api", "type", "click"),
                Map.of("id", "a2", "source", "api", "type", "view")
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("events", events);
        result.getOutputData().put("count", 2);
        return result;
    }
}
