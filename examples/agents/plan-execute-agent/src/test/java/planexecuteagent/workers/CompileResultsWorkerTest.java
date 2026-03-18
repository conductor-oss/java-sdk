package planexecuteagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompileResultsWorkerTest {

    private final CompileResultsWorker worker = new CompileResultsWorker();

    @Test
    void taskDefName() {
        assertEquals("pe_compile_results", worker.getTaskDefName());
    }

    @Test
    void compilesAllResults() {
        Task task = taskWith(Map.of(
                "objective", "Develop go-to-market strategy for new SaaS product",
                "result1", "Collected data on 5 competitors; market size estimated at $4.2B",
                "result2", "Identified 3 growth opportunities: API platform, enterprise tier, international expansion",
                "result3", "Recommend prioritizing API platform (ROI: capacity-planning%) followed by enterprise tier (ROI: 210%)"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("report"));
    }

    @Test
    void reportContainsObjective() {
        Task task = taskWith(Map.of(
                "objective", "Develop go-to-market strategy for new SaaS product",
                "result1", "data1",
                "result2", "data2",
                "result3", "data3"));
        TaskResult result = worker.execute(task);

        String report = (String) result.getOutputData().get("report");
        assertTrue(report.contains("Develop go-to-market strategy for new SaaS product"));
    }

    @Test
    void reportContainsAllStepResults() {
        Task task = taskWith(Map.of(
                "objective", "Strategy",
                "result1", "Collected data on 5 competitors",
                "result2", "Identified 3 growth opportunities",
                "result3", "Recommend prioritizing API platform"));
        TaskResult result = worker.execute(task);

        String report = (String) result.getOutputData().get("report");
        assertTrue(report.contains("Collected data on 5 competitors"));
        assertTrue(report.contains("Identified 3 growth opportunities"));
        assertTrue(report.contains("Recommend prioritizing API platform"));
    }

    @Test
    void reportHasCorrectFormat() {
        Task task = taskWith(Map.of(
                "objective", "My Objective",
                "result1", "R1",
                "result2", "R2",
                "result3", "R3"));
        TaskResult result = worker.execute(task);

        String report = (String) result.getOutputData().get("report");
        assertEquals("Objective: My Objective | Step 1: R1 | Step 2: R2 | Step 3: R3", report);
    }

    @Test
    void handlesEmptyObjective() {
        Map<String, Object> input = new HashMap<>();
        input.put("objective", "");
        input.put("result1", "R1");
        input.put("result2", "R2");
        input.put("result3", "R3");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("report"));
    }

    @Test
    void handlesMissingResults() {
        Task task = taskWith(Map.of("objective", "Strategy"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String report = (String) result.getOutputData().get("report");
        assertNotNull(report);
        assertTrue(report.contains("Strategy"));
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("objective", null);
        input.put("result1", null);
        input.put("result2", null);
        input.put("result3", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("report"));
    }

    @Test
    void handlesAllEmptyInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("report"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
