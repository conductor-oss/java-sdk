package eventversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Detects the schema version of an incoming event.
 * Input: event (map)
 * Output: version (from event.version, event.schemaVersion, or default "v3")
 */
public class DetectVersionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vr_detect_version";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object eventRaw = task.getInputData().get("event");
        Map<String, Object> event = (eventRaw instanceof Map)
                ? (Map<String, Object>) eventRaw
                : Map.of();

        String version;
        if (event.get("version") != null) {
            version = String.valueOf(event.get("version"));
        } else if (event.get("schemaVersion") != null) {
            version = String.valueOf(event.get("schemaVersion"));
        } else {
            version = "v3";
        }

        System.out.println("  [vr_detect_version] Event version: " + version);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("version", version);
        return result;
    }
}
