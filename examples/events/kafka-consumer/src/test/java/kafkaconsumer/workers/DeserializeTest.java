package kafkaconsumer.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DeserializeTest {

    private final Deserialize worker = new Deserialize();

    @Test
    void taskDefName() {
        assertEquals("kc_deserialize", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("key", "U-4421", "value", "{}"), "json");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsDeserializedData() {
        Task task = taskWith(Map.of("key", "U-4421", "value", "{}"), "json");
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getOutputData().get("deserializedData");
        assertNotNull(data);
        assertEquals("U-4421", data.get("userId"));
        assertEquals("profile_updated", data.get("action"));
        assertEquals("user-service", data.get("source"));
    }

    @Test
    void deserializedDataContainsChanges() {
        Task task = taskWith(Map.of("key", "U-4421", "value", "{}"), "json");
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getOutputData().get("deserializedData");
        @SuppressWarnings("unchecked")
        Map<String, Object> changes = (Map<String, Object>) data.get("changes");
        assertNotNull(changes);
        assertEquals("Jane Doe", changes.get("displayName"));
        assertEquals("America/New_York", changes.get("timezone"));
    }

    @Test
    void outputContainsMessageType() {
        Task task = taskWith(Map.of("key", "U-4421", "value", "{}"), "json");
        TaskResult result = worker.execute(task);
        assertEquals("user.profile.updated", result.getOutputData().get("messageType"));
    }

    @Test
    void outputContainsSchemaVersion() {
        Task task = taskWith(Map.of("key", "U-4421", "value", "{}"), "json");
        TaskResult result = worker.execute(task);
        assertEquals(2, result.getOutputData().get("schemaVersion"));
    }

    @Test
    void deterministicOutputForSameInput() {
        Task task1 = taskWith(Map.of("key", "k1", "value", "v1"), "json");
        Task task2 = taskWith(Map.of("key", "k1", "value", "v1"), "json");

        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("deserializedData"), r2.getOutputData().get("deserializedData"));
        assertEquals(r1.getOutputData().get("messageType"), r2.getOutputData().get("messageType"));
        assertEquals(r1.getOutputData().get("schemaVersion"), r2.getOutputData().get("schemaVersion"));
    }

    @Test
    void acceptsDifferentFormats() {
        Task task = taskWith(Map.of("key", "k", "value", "v"), "avro");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("deserializedData"));
    }

    private Task taskWith(Map<String, Object> rawMessage, String format) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("rawMessage", rawMessage);
        input.put("format", format);
        task.setInputData(input);
        return task;
    }
}
