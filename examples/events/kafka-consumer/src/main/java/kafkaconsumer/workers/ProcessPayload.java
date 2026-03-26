package kafkaconsumer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Processes the deserialized Kafka message payload and applies the relevant action.
 */
public class ProcessPayload implements Worker {

    @Override
    public String getTaskDefName() {
        return "kc_process_payload";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> deserializedData = (Map<String, Object>) task.getInputData().get("deserializedData");
        String messageType = (String) task.getInputData().get("messageType");

        System.out.println("[kc_process_payload] Processing messageType=" + messageType);

        TaskResult result = new TaskResult(task);

        String userId = deserializedData != null ? (String) deserializedData.get("userId") : "unknown";

        result.getOutputData().put("processed", true);
        result.getOutputData().put("result", "applied");
        result.getOutputData().put("affectedEntities", List.of(userId));
        result.getOutputData().put("processingTimeMs", 42);

        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }
}
