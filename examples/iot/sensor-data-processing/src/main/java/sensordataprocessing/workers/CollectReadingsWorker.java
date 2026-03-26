package sensordataprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Collects sensor readings from a sensor group within a time window.
 * Input: batchId, sensorGroupId, timeWindowMinutes
 * Output: readingCount, readings, sensorCount
 */
public class CollectReadingsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sen_collect_readings";
    }

    @Override
    public TaskResult execute(Task task) {
        String sensorGroupId = (String) task.getInputData().get("sensorGroupId");
        if (sensorGroupId == null) {
            sensorGroupId = "unknown";
        }

        System.out.println("  [collect] Collecting readings from sensor group " + sensorGroupId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("readingCount", 1200);
        result.getOutputData().put("readings", List.of(
                Map.of("sensorId", "S-001", "temperature", 72.5, "humidity", 45, "timestamp", "2026-03-08T10:00:00Z"),
                Map.of("sensorId", "S-002", "temperature", 74.1, "humidity", 42, "timestamp", "2026-03-08T10:00:00Z"),
                Map.of("sensorId", "S-003", "temperature", 89.2, "humidity", 38, "timestamp", "2026-03-08T10:00:00Z")
        ));
        result.getOutputData().put("sensorCount", 50);
        return result;
    }
}
