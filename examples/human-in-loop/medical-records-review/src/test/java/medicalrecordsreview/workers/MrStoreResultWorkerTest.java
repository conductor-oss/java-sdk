package medicalrecordsreview.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MrStoreResultWorkerTest {

    @Test
    void taskDefName() {
        MrStoreResultWorker worker = new MrStoreResultWorker();
        assertEquals("mr_store_result", worker.getTaskDefName());
    }

    @Test
    void returnsStoredTrue() {
        MrStoreResultWorker worker = new MrStoreResultWorker();
        Task task = taskWith(Map.of("recordId", "REC-001"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("stored"));
    }

    @Test
    void returnsDeterministicAuditTrailId() {
        MrStoreResultWorker worker = new MrStoreResultWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertEquals("AUDIT-001", result.getOutputData().get("auditTrailId"));
    }

    @Test
    void outputContainsStoredAndAuditTrailIdKeys() {
        MrStoreResultWorker worker = new MrStoreResultWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("stored"));
        assertTrue(result.getOutputData().containsKey("auditTrailId"));
    }

    @Test
    void alwaysCompletes() {
        MrStoreResultWorker worker = new MrStoreResultWorker();
        Task task = taskWith(Map.of("extra", "data"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("stored"));
        assertEquals("AUDIT-001", result.getOutputData().get("auditTrailId"));
    }

    @Test
    void auditTrailIdIsConsistentAcrossCalls() {
        MrStoreResultWorker worker = new MrStoreResultWorker();

        Task task1 = taskWith(Map.of());
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of());
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("auditTrailId"),
                     result2.getOutputData().get("auditTrailId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
