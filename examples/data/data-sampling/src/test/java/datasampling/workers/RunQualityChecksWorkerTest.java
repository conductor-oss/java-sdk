package datasampling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RunQualityChecksWorkerTest {

    private final RunQualityChecksWorker worker = new RunQualityChecksWorker();

    @Test
    void taskDefName() {
        assertEquals("sm_run_quality_checks", worker.getTaskDefName());
    }

    @Test
    void allValidRecordsPass() {
        List<Map<String, Object>> sample = List.of(
                Map.of("name", "Alice", "value", 95),
                Map.of("name", "Bob", "value", 82));
        Task task = taskWith(Map.of("sample", sample, "threshold", 0.8));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1.0, result.getOutputData().get("qualityScore"));
        assertEquals("pass", result.getOutputData().get("decision"));
        assertEquals(2, result.getOutputData().get("validCount"));
        assertEquals(2, result.getOutputData().get("checkedCount"));
        @SuppressWarnings("unchecked")
        List<String> issues = (List<String>) result.getOutputData().get("issues");
        assertTrue(issues.isEmpty());
    }

    @Test
    void recordWithEmptyNameFails() {
        List<Map<String, Object>> sample = List.of(
                Map.of("name", "", "value", 50),
                Map.of("name", "Bob", "value", 82));
        Task task = taskWith(Map.of("sample", sample, "threshold", 0.8));
        TaskResult result = worker.execute(task);

        assertEquals(0.5, result.getOutputData().get("qualityScore"));
        assertEquals("fail", result.getOutputData().get("decision"));
        assertEquals(1, result.getOutputData().get("validCount"));
    }

    @Test
    void recordWithNegativeValueFails() {
        List<Map<String, Object>> sample = List.of(
                Map.of("name", "Alice", "value", -5),
                Map.of("name", "Bob", "value", 82));
        Task task = taskWith(Map.of("sample", sample, "threshold", 0.8));
        TaskResult result = worker.execute(task);

        assertEquals(0.5, result.getOutputData().get("qualityScore"));
        assertEquals("fail", result.getOutputData().get("decision"));
        @SuppressWarnings("unchecked")
        List<String> issues = (List<String>) result.getOutputData().get("issues");
        assertEquals(1, issues.size());
        assertTrue(issues.get(0).contains("negative value"));
    }

    @Test
    void recordWithMissingNameAndValueFailsBoth() {
        Map<String, Object> badRecord = new HashMap<>();
        // no "name" or "value" keys
        List<Map<String, Object>> sample = List.of(badRecord);
        Task task = taskWith(Map.of("sample", sample, "threshold", 0.5));
        TaskResult result = worker.execute(task);

        assertEquals(0.0, result.getOutputData().get("qualityScore"));
        assertEquals("fail", result.getOutputData().get("decision"));
        @SuppressWarnings("unchecked")
        List<String> issues = (List<String>) result.getOutputData().get("issues");
        assertEquals(2, issues.size());
    }

    @Test
    void emptySampleReturnsZeroScoreAndFail() {
        Task task = taskWith(Map.of("sample", List.of(), "threshold", 0.8));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, result.getOutputData().get("qualityScore"));
        assertEquals("fail", result.getOutputData().get("decision"));
        assertEquals(0, result.getOutputData().get("validCount"));
        assertEquals(0, result.getOutputData().get("checkedCount"));
    }

    @Test
    void handlesNullSample() {
        Map<String, Object> input = new HashMap<>();
        input.put("sample", null);
        input.put("threshold", 0.8);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, result.getOutputData().get("qualityScore"));
        assertEquals("fail", result.getOutputData().get("decision"));
    }

    @Test
    void passesAtExactThreshold() {
        // 2 of 4 valid = 0.5, threshold = 0.5 -> pass
        List<Map<String, Object>> sample = List.of(
                Map.of("name", "Alice", "value", 10),
                Map.of("name", "", "value", 20),
                Map.of("name", "Charlie", "value", 30),
                Map.of("name", "", "value", 40));
        Task task = taskWith(Map.of("sample", sample, "threshold", 0.5));
        TaskResult result = worker.execute(task);

        assertEquals(0.5, result.getOutputData().get("qualityScore"));
        assertEquals("pass", result.getOutputData().get("decision"));
    }

    @Test
    void nonNumericValueCreatesIssue() {
        List<Map<String, Object>> sample = List.of(
                Map.of("name", "Alice", "value", "not-a-number"));
        Task task = taskWith(Map.of("sample", sample, "threshold", 0.5));
        TaskResult result = worker.execute(task);

        assertEquals(0.0, result.getOutputData().get("qualityScore"));
        assertEquals("fail", result.getOutputData().get("decision"));
        @SuppressWarnings("unchecked")
        List<String> issues = (List<String>) result.getOutputData().get("issues");
        assertEquals(1, issues.size());
        assertTrue(issues.get(0).contains("non-numeric"));
    }

    @Test
    void zeroValueIsValid() {
        List<Map<String, Object>> sample = List.of(
                Map.of("name", "Alice", "value", 0));
        Task task = taskWith(Map.of("sample", sample, "threshold", 0.8));
        TaskResult result = worker.execute(task);

        assertEquals(1.0, result.getOutputData().get("qualityScore"));
        assertEquals("pass", result.getOutputData().get("decision"));
        assertEquals(1, result.getOutputData().get("validCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
