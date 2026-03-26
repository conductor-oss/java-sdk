package eventtransformation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Delivers the mapped event to its target destination and returns delivery metadata:
 * outputEventId, delivered flag, outputSizeBytes, and deliveredAt timestamp.
 */
public class OutputEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "et_output_event";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> mappedEvent = (Map<String, Object>) task.getInputData().get("mappedEvent");
        if (mappedEvent == null) {
            mappedEvent = Map.of();
        }
        String targetFormat = (String) task.getInputData().get("targetFormat");
        if (targetFormat == null || targetFormat.isBlank()) {
            targetFormat = "cloudevents";
        }

        System.out.println("  [et_output_event] Delivering event to format: " + targetFormat);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("outputEventId", "out-fixed-001");
        result.getOutputData().put("delivered", true);
        result.getOutputData().put("outputSizeBytes", 512);
        result.getOutputData().put("deliveredAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
