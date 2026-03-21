package kafkaconsumer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deserializes a raw Kafka message into structured data with type and schema information.
 */
public class Deserialize implements Worker {

    @Override
    public String getTaskDefName() {
        return "kc_deserialize";
    }

    @Override
    public TaskResult execute(Task task) {
        Object rawMessage = task.getInputData().get("rawMessage");
        String format = (String) task.getInputData().get("format");

        System.out.println("[kc_deserialize] Deserializing message in format=" + format);

        TaskResult result = new TaskResult(task);

        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("displayName", "Jane Doe");
        changes.put("timezone", "America/New_York");

        Map<String, Object> deserializedData = new LinkedHashMap<>();
        deserializedData.put("userId", "U-4421");
        deserializedData.put("action", "profile_updated");
        deserializedData.put("changes", changes);
        deserializedData.put("source", "user-service");

        result.getOutputData().put("deserializedData", deserializedData);
        result.getOutputData().put("messageType", "user.profile.updated");
        result.getOutputData().put("schemaVersion", 2);

        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }
}
