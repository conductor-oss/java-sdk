package eventversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Transforms a v1 event to the latest (v3) schema format.
 * Input: event (map)
 * Output: transformed (event merged with version:"v3", migratedFrom:"v1")
 */
public class TransformV1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vr_transform_v1";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object eventRaw = task.getInputData().get("event");
        Map<String, Object> event = (eventRaw instanceof Map)
                ? (Map<String, Object>) eventRaw
                : Map.of();

        System.out.println("  [vr_transform_v1] Converting v1 event to v3 format");

        Map<String, Object> transformed = new HashMap<>(event);
        transformed.put("version", "v3");
        transformed.put("migratedFrom", "v1");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transformed", transformed);
        return result;
    }
}
