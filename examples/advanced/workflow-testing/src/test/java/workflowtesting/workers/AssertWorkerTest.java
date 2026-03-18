package workflowtesting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssertWorkerTest {

    private final AssertWorker worker = new AssertWorker();

    @Test
    void taskDefName() {
        assertEquals("wft_assert", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of(
                "actualOutput", Map.of("status", "SUCCESS", "processed", 2),
                "expectedOutput", Map.of("status", "SUCCESS", "processed", 2)));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void allPassedWhenMatching() {
        Task task = taskWith(Map.of(
                "actualOutput", Map.of("status", "SUCCESS", "processed", 2),
                "expectedOutput", Map.of("status", "SUCCESS", "processed", 2)));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("allPassed"));
    }

    @Test
    void allPassedFalseWhenMismatch() {
        Task task = taskWith(Map.of(
                "actualOutput", Map.of("status", "FAILED", "processed", 1),
                "expectedOutput", Map.of("status", "SUCCESS", "processed", 2)));
        TaskResult result = worker.execute(task);
        assertEquals(false, result.getOutputData().get("allPassed"));
    }

    @Test
    void assertionsListHasTwoEntries() {
        Task task = taskWith(Map.of(
                "actualOutput", Map.of("status", "SUCCESS", "processed", 2),
                "expectedOutput", Map.of("status", "SUCCESS", "processed", 2)));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> assertions = (List<Map<String, Object>>) result.getOutputData().get("assertions");
        assertEquals(2, assertions.size());
    }

    @Test
    void statusCheckAssertionPresent() {
        Task task = taskWith(Map.of(
                "actualOutput", Map.of("status", "SUCCESS", "processed", 2),
                "expectedOutput", Map.of("status", "SUCCESS", "processed", 2)));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> assertions = (List<Map<String, Object>>) result.getOutputData().get("assertions");
        assertEquals("status_check", assertions.get(0).get("name"));
        assertEquals(true, assertions.get(0).get("passed"));
    }

    @Test
    void countCheckAssertionPresent() {
        Task task = taskWith(Map.of(
                "actualOutput", Map.of("status", "SUCCESS", "processed", 2),
                "expectedOutput", Map.of("status", "SUCCESS", "processed", 2)));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> assertions = (List<Map<String, Object>>) result.getOutputData().get("assertions");
        assertEquals("count_check", assertions.get(1).get("name"));
        assertEquals(true, assertions.get(1).get("passed"));
    }

    @Test
    void handlesNullActualOutput() {
        Map<String, Object> input = new HashMap<>();
        input.put("actualOutput", null);
        input.put("expectedOutput", Map.of("status", "SUCCESS"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("allPassed"));
    }

    @Test
    void handlesNullExpectedOutput() {
        Map<String, Object> input = new HashMap<>();
        input.put("actualOutput", Map.of("status", "SUCCESS"));
        input.put("expectedOutput", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("assertions"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
