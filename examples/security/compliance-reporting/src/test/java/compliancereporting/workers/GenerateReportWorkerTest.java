package compliancereporting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateReportWorkerTest {

    private final GenerateReportWorker worker = new GenerateReportWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_generate_report", worker.getTaskDefName());
    }

    @Test
    void generatesReportWithGapsData() {
        Task task = taskWith(Map.of("generate_reportData", Map.of("assess_gaps", true)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("generate_report"));
        assertEquals("2026-01-15T10:05:00Z", result.getOutputData().get("completedAt"));
    }

    @Test
    void generatesReportWithEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("generate_report"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("generate_reportData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsCompletedAt() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("completedAt"));
        assertEquals("2026-01-15T10:05:00Z", result.getOutputData().get("completedAt"));
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of("generate_reportData", "data"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("generate_report"));
        assertTrue(result.getOutputData().containsKey("completedAt"));
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void generateReportValueIsTrue() {
        Task task = taskWith(Map.of("generate_reportData", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("generate_report"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
