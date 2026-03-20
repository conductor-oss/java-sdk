package gdprdatadeletion.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FindRecordsWorkerTest {

    private final FindRecordsWorker worker = new FindRecordsWorker();

    @Test
    void taskDefName() {
        assertEquals("gr_find_records", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("userId", "USR-001"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsRecords() {
        Task task = taskWith(Map.of("userId", "USR-001"));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("records"));
    }

    @Test
    void returnsRecordCount() {
        Task task = taskWith(Map.of("userId", "USR-001"));
        TaskResult result = worker.execute(task);
        assertEquals(7, result.getOutputData().get("recordCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsSystems() {
        Task task = taskWith(Map.of("userId", "USR-001"));
        TaskResult result = worker.execute(task);
        List<String> systems = (List<String>) result.getOutputData().get("systems");
        assertNotNull(systems);
        assertTrue(systems.contains("user_accounts"));
        assertTrue(systems.contains("analytics"));
        assertTrue(systems.contains("billing"));
    }

    @Test
    void systemsAreUnique() {
        Task task = taskWith(Map.of("userId", "USR-001"));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        List<String> systems = (List<String>) result.getOutputData().get("systems");
        assertEquals(systems.size(), systems.stream().distinct().count());
    }

    @Test
    void handlesDefaultUserId() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
