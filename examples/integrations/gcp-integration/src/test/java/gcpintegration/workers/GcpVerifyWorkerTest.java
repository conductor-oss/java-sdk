package gcpintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GcpVerifyWorkerTest {

    private final GcpVerifyWorker worker = new GcpVerifyWorker();

    @Test
    void taskDefName() {
        assertEquals("gcp_verify", worker.getTaskDefName());
    }

    @Test
    void verifiesAllServicesPresent() {
        Task task = taskWith(Map.of(
                "gcsObject", "gs://bucket/file.json",
                "firestoreDoc", "evt-6001",
                "pubsubMsg", "pubsub-msg-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
    }

    @Test
    void failsWhenGcsMissing() {
        Map<String, Object> input = new HashMap<>();
        input.put("gcsObject", null);
        input.put("firestoreDoc", "doc-1");
        input.put("pubsubMsg", "msg-1");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void failsWhenFirestoreMissing() {
        Map<String, Object> input = new HashMap<>();
        input.put("gcsObject", "gs://b/f");
        input.put("firestoreDoc", null);
        input.put("pubsubMsg", "msg-1");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void failsWhenPubsubMissing() {
        Map<String, Object> input = new HashMap<>();
        input.put("gcsObject", "gs://b/f");
        input.put("firestoreDoc", "doc-1");
        input.put("pubsubMsg", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void failsWhenAllMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void failsWithEmptyStrings() {
        Task task = taskWith(Map.of("gcsObject", "", "firestoreDoc", "", "pubsubMsg", ""));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void passesWithDifferentValues() {
        Task task = taskWith(Map.of(
                "gcsObject", "gs://other/path.json",
                "firestoreDoc", "doc-999",
                "pubsubMsg", "msg-abc"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("verified"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
