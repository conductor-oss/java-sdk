package incrementalrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectChangesWorkerTest {

    private final DetectChangesWorker worker = new DetectChangesWorker();

    @Test
    void taskDefName() {
        assertEquals("ir_detect_changes", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsChangedDocIds() {
        Task task = taskWith(Map.of(
                "sourceCollection", "knowledge_base",
                "lastSyncTimestamp", "2025-01-01T00:00:00Z"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<String> changedDocIds = (List<String>) result.getOutputData().get("changedDocIds");
        assertNotNull(changedDocIds);
        assertEquals(4, changedDocIds.size());
        assertTrue(changedDocIds.contains("doc-101"));
        assertTrue(changedDocIds.contains("doc-205"));
        assertTrue(changedDocIds.contains("doc-307"));
        assertTrue(changedDocIds.contains("doc-412"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsExistingHashes() {
        Task task = taskWith(Map.of(
                "sourceCollection", "knowledge_base",
                "lastSyncTimestamp", "2025-01-01T00:00:00Z"
        ));
        TaskResult result = worker.execute(task);

        Map<String, String> hashes = (Map<String, String>) result.getOutputData().get("existingHashes");
        assertNotNull(hashes);
        assertEquals(4, hashes.size());
        assertEquals("a1b2c3", hashes.get("doc-101"));
        assertNull(hashes.get("doc-205"));
        assertEquals("d4e5f6", hashes.get("doc-307"));
        assertNull(hashes.get("doc-412"));
    }

    @Test
    void returnsTotalChanged() {
        Task task = taskWith(Map.of(
                "sourceCollection", "knowledge_base",
                "lastSyncTimestamp", "2025-01-01T00:00:00Z"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(4, result.getOutputData().get("totalChanged"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
