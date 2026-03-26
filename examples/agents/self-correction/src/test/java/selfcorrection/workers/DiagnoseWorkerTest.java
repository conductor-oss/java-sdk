package selfcorrection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiagnoseWorkerTest {

    private final DiagnoseWorker worker = new DiagnoseWorker();

    @Test
    void taskDefName() {
        assertEquals("sc_diagnose", worker.getTaskDefName());
    }

    @Test
    void returnsDiagnosis() {
        Task task = taskWith(Map.of(
                "code", "function fibonacci(n) { return n; }",
                "errors", List.of("TypeError: Maximum call stack exceeded")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("diagnosis"));
    }

    @Test
    void diagnosisReferencesNegativeNumbers() {
        Task task = taskWith(Map.of(
                "code", "function fibonacci(n) { return n; }",
                "errors", List.of("TypeError: Maximum call stack exceeded")));
        TaskResult result = worker.execute(task);

        String diagnosis = (String) result.getOutputData().get("diagnosis");
        assertTrue(diagnosis.contains("negative numbers"));
    }

    @Test
    void diagnosisReferencesInfiniteRecursion() {
        Task task = taskWith(Map.of(
                "code", "function fibonacci(n) { return n; }",
                "errors", List.of("TypeError: Maximum call stack exceeded")));
        TaskResult result = worker.execute(task);

        String diagnosis = (String) result.getOutputData().get("diagnosis");
        assertTrue(diagnosis.contains("recurses infinitely"));
    }

    @Test
    void returnsSeverity() {
        Task task = taskWith(Map.of(
                "code", "function fibonacci(n) { return n; }",
                "errors", List.of("TypeError: Maximum call stack exceeded")));
        TaskResult result = worker.execute(task);

        assertEquals("high", result.getOutputData().get("severity"));
    }

    @Test
    void handlesEmptyErrors() {
        Map<String, Object> input = new HashMap<>();
        input.put("code", "function fibonacci(n) { return n; }");
        input.put("errors", List.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("diagnosis"));
    }

    @Test
    void handlesNullErrors() {
        Map<String, Object> input = new HashMap<>();
        input.put("code", "function fibonacci(n) { return n; }");
        input.put("errors", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("diagnosis"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("diagnosis"));
        assertNotNull(result.getOutputData().get("severity"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
