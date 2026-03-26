package datasync.workers;

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
        assertEquals("sy_detect_changes", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsChangeCounts() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(3, result.getOutputData().get("changeCountA"));
        assertEquals(3, result.getOutputData().get("changeCountB"));
    }

    @Test
    void returnsConflicts() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(1, result.getOutputData().get("conflictCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void changesHaveRequiredFields() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        List<Map<String, Object>> changes = (List<Map<String, Object>>) result.getOutputData().get("changesInA");
        for (Map<String, Object> c : changes) {
            assertNotNull(c.get("recordId"));
            assertNotNull(c.get("field"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
