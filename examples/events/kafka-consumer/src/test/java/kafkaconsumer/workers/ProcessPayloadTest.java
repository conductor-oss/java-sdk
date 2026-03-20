package kafkaconsumer.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProcessPayloadTest {

    private final ProcessPayload worker = new ProcessPayload();

    @Test
    void taskDefName() {
        assertEquals("kc_process_payload", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("userId", "U-4421", "action", "profile_updated"), "user.profile.updated");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputProcessedIsTrue() {
        Task task = taskWith(Map.of("userId", "U-4421", "action", "profile_updated"), "user.profile.updated");
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputResultIsApplied() {
        Task task = taskWith(Map.of("userId", "U-4421", "action", "profile_updated"), "user.profile.updated");
        TaskResult result = worker.execute(task);
        assertEquals("applied", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsAffectedEntities() {
        Task task = taskWith(Map.of("userId", "U-4421", "action", "profile_updated"), "user.profile.updated");
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> entities = (List<String>) result.getOutputData().get("affectedEntities");
        assertNotNull(entities);
        assertEquals(1, entities.size());
        assertEquals("U-4421", entities.get(0));
    }

    @Test
    void outputContainsProcessingTimeMs() {
        Task task = taskWith(Map.of("userId", "U-4421", "action", "profile_updated"), "user.profile.updated");
        TaskResult result = worker.execute(task);
        assertEquals(42, result.getOutputData().get("processingTimeMs"));
    }

    @Test
    void affectedEntitiesUsesUserIdFromInput() {
        Task task = taskWith(Map.of("userId", "U-9999", "action", "delete"), "user.deleted");
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> entities = (List<String>) result.getOutputData().get("affectedEntities");
        assertEquals("U-9999", entities.get(0));
    }

    @Test
    void deterministicOutputForSameInput() {
        Map<String, Object> data = Map.of("userId", "U-4421", "action", "profile_updated");
        Task task1 = taskWith(data, "user.profile.updated");
        Task task2 = taskWith(data, "user.profile.updated");

        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("processed"), r2.getOutputData().get("processed"));
        assertEquals(r1.getOutputData().get("result"), r2.getOutputData().get("result"));
        assertEquals(r1.getOutputData().get("processingTimeMs"), r2.getOutputData().get("processingTimeMs"));
    }

    @Test
    void handlesNullDeserializedDataGracefully() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("deserializedData", null);
        input.put("messageType", "unknown");
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<String> entities = (List<String>) result.getOutputData().get("affectedEntities");
        assertEquals("unknown", entities.get(0));
    }

    private Task taskWith(Map<String, Object> deserializedData, String messageType) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("deserializedData", new HashMap<>(deserializedData));
        input.put("messageType", messageType);
        task.setInputData(input);
        return task;
    }
}
