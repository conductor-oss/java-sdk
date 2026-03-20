package eventmerge.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Collects events from stream B (mobile source).
 * Input: source
 * Output: events (list of event maps), count
 */
public class CollectStreamBWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mg_collect_stream_b";
    }

    @Override
    public TaskResult execute(Task task) {
        String source = (String) task.getInputData().get("source");
        if (source == null) {
            source = "mobile";
        }

        System.out.println("  [mg_collect_stream_b] Collecting from source: " + source);

        List<Map<String, String>> events = List.of(
                Map.of("id", "b1", "source", "mobile", "type", "tap"),
                Map.of("id", "b2", "source", "mobile", "type", "scroll"),
                Map.of("id", "b3", "source", "mobile", "type", "tap")
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("events", events);
        result.getOutputData().put("count", 3);
        return result;
    }
}
