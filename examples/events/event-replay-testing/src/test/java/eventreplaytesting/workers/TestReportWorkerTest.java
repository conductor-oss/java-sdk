package eventreplaytesting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestReportWorkerTest {

    private final TestReportWorker worker = new TestReportWorker();

    @Test
    void taskDefName() {
        assertEquals("rt_test_report", worker.getTaskDefName());
    }

    @Test
    void generatesReportSuccessfully() {
        Task task = taskWith(Map.of(
                "testSuiteId", "suite-001",
                "totalReplayed", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void statusIsAllPassed() {
        Task task = taskWith(Map.of(
                "testSuiteId", "suite-002",
                "totalReplayed", 3));
        TaskResult result = worker.execute(task);

        assertEquals("all_passed", result.getOutputData().get("status"));
    }

    @Test
    void passCountMatchesTotalReplayed() {
        Task task = taskWith(Map.of(
                "testSuiteId", "suite-003",
                "totalReplayed", 3));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("passCount"));
    }

    @Test
    void failCountIsZero() {
        Task task = taskWith(Map.of(
                "testSuiteId", "suite-004",
                "totalReplayed", 3));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("failCount"));
    }

    @Test
    void handlesZeroReplayed() {
        Task task = taskWith(Map.of(
                "testSuiteId", "suite-005",
                "totalReplayed", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("passCount"));
        assertEquals(0, result.getOutputData().get("failCount"));
    }

    @Test
    void handlesNullTestSuiteId() {
        Map<String, Object> input = new HashMap<>();
        input.put("testSuiteId", null);
        input.put("totalReplayed", 3);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("all_passed", result.getOutputData().get("status"));
    }

    @Test
    void handlesNullTotalReplayed() {
        Map<String, Object> input = new HashMap<>();
        input.put("testSuiteId", "suite-006");
        input.put("totalReplayed", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("passCount"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("all_passed", result.getOutputData().get("status"));
        assertEquals(0, result.getOutputData().get("passCount"));
        assertEquals(0, result.getOutputData().get("failCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
