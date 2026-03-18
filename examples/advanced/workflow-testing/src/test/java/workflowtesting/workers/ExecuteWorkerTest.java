package workflowtesting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteWorkerTest {

    private final ExecuteWorker worker = new ExecuteWorker();

    @Test
    void taskDefName() {
        assertEquals("wft_execute", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("workflowUnderTest", "wf1", "fixtures", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsActualOutput() {
        Task task = taskWith(Map.of("workflowUnderTest", "test_wf",
                "fixtures", Map.of("testData", List.of(
                        Map.of("id", 1, "value", "alpha"),
                        Map.of("id", 2, "value", "beta")))));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("actualOutput"));
    }

    @Test
    void actualOutputHasProcessedCount() {
        Task task = taskWith(Map.of("workflowUnderTest", "test_wf",
                "fixtures", Map.of("testData", List.of(
                        Map.of("id", 1, "value", "alpha"),
                        Map.of("id", 2, "value", "beta")))));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> actual = (Map<String, Object>) result.getOutputData().get("actualOutput");
        assertEquals(2, actual.get("processed"));
    }

    @Test
    void actualOutputHasSuccessStatus() {
        Task task = taskWith(Map.of("workflowUnderTest", "test_wf",
                "fixtures", Map.of("testData", List.of(
                        Map.of("id", 1, "value", "alpha")))));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> actual = (Map<String, Object>) result.getOutputData().get("actualOutput");
        assertEquals("SUCCESS", actual.get("status"));
    }

    @Test
    void actualOutputContainsProcessedResults() {
        Task task = taskWith(Map.of("workflowUnderTest", "test_wf",
                "fixtures", Map.of("testData", List.of(
                        Map.of("id", 1, "value", "alpha"),
                        Map.of("id", 2, "value", "beta")))));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> actual = (Map<String, Object>) result.getOutputData().get("actualOutput");
        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) actual.get("results");
        assertEquals(2, results.size());
        assertTrue(results.contains("alpha_processed"));
        assertTrue(results.contains("beta_processed"));
    }

    @Test
    void outputContainsExecutionTime() {
        Task task = taskWith(Map.of("workflowUnderTest", "test_wf",
                "fixtures", Map.of("testData", List.of(Map.of("id", 1, "value", "x")))));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("executionTimeMs"));
        assertTrue(((Number) result.getOutputData().get("executionTimeMs")).longValue() >= 0);
    }

    @Test
    void noDataReturnsNoDataStatus() {
        Task task = taskWith(Map.of("workflowUnderTest", "empty_wf",
                "fixtures", Map.of("testData", List.of())));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> actual = (Map<String, Object>) result.getOutputData().get("actualOutput");
        assertEquals("NO_DATA", actual.get("status"));
        assertEquals(0, actual.get("processed"));
    }

    @Test
    void handlesNullWorkflowName() {
        Map<String, Object> input = new HashMap<>();
        input.put("workflowUnderTest", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("actualOutput"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
