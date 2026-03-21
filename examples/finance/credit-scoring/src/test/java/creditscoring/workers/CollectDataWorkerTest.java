package creditscoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectDataWorkerTest {

    private final CollectDataWorker worker = new CollectDataWorker();

    @Test
    void taskDefName() {
        assertEquals("csc_collect_data", worker.getTaskDefName());
    }

    @Test
    void returnsCreditHistory() {
        Task task = taskWith(Map.of("applicantId", "APP-001", "ssn", "XXX-XX-1234"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("creditHistory"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void creditHistoryContainsAccountAge() {
        Task task = taskWith(Map.of("applicantId", "APP-002", "ssn", "XXX-XX-5678"));
        TaskResult result = worker.execute(task);

        Map<String, Object> history = (Map<String, Object>) result.getOutputData().get("creditHistory");
        assertEquals(12, history.get("accountAge"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void creditHistoryContainsUtilization() {
        Task task = taskWith(Map.of("applicantId", "APP-003", "ssn", "XXX-XX-9999"));
        TaskResult result = worker.execute(task);

        Map<String, Object> history = (Map<String, Object>) result.getOutputData().get("creditHistory");
        assertEquals(28, history.get("utilization"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void creditHistoryContainsTotalAccounts() {
        Task task = taskWith(Map.of("applicantId", "APP-004", "ssn", "XXX-XX-1111"));
        TaskResult result = worker.execute(task);

        Map<String, Object> history = (Map<String, Object>) result.getOutputData().get("creditHistory");
        assertEquals(8, history.get("totalAccounts"));
    }

    @Test
    void handlesNullApplicantId() {
        Map<String, Object> input = new HashMap<>();
        input.put("applicantId", null);
        input.put("ssn", "XXX-XX-0000");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("creditHistory"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void creditHistoryContainsZeroCollections() {
        Task task = taskWith(Map.of("applicantId", "APP-005", "ssn", "XXX-XX-2222"));
        TaskResult result = worker.execute(task);

        Map<String, Object> history = (Map<String, Object>) result.getOutputData().get("creditHistory");
        assertEquals(0, history.get("collections"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
