package eventmerge.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Collects events from stream C (IoT source).
 * Input: source
 * Output: events (list of event maps), count
 */
public class CollectStreamCWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mg_collect_stream_c";
    }

    @Override
    public TaskResult execute(Task task) {
        String source = (String) task.getInputData().get("source");
        if (source == null) {
            source = "iot";
        }

        System.out.println("  [mg_collect_stream_c] Collecting from source: " + source);

        List<Map<String, String>> events = List.of(
                Map.of("id", "c1", "source", "iot", "type", "sensor_reading")
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("events", events);
        result.getOutputData().put("count", 1);
        return result;
    }
}
