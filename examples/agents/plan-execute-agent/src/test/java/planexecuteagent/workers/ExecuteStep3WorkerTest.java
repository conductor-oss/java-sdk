package planexecuteagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteStep3WorkerTest {

    private final ExecuteStep3Worker worker = new ExecuteStep3Worker();

    @Test
    void taskDefName() {
        assertEquals("pe_execute_step_3", worker.getTaskDefName());
    }

    @Test
    void executesStepSuccessfully() {
        Task task = taskWith(Map.of(
                "step", "Generate strategic recommendations",
                "stepIndex", 2,
                "previousResult", "Identified 3 growth opportunities"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsRecommendationResult() {
        Task task = taskWith(Map.of(
                "step", "Generate strategic recommendations",
                "stepIndex", 2,
                "previousResult", "some analysis"));
        TaskResult result = worker.execute(task);

        String output = (String) result.getOutputData().get("result");
        assertEquals("Recommend prioritizing API platform (ROI: capacity-planning%) followed by enterprise tier (ROI: 210%)", output);
    }

    @Test
    void returnsCompleteStatus() {
        Task task = taskWith(Map.of(
                "step", "Generate recommendations",
                "stepIndex", 2,
                "previousResult", "analysis data"));
        TaskResult result = worker.execute(task);

        assertEquals("complete", result.getOutputData().get("status"));
    }

    @Test
    void handlesEmptyStep() {
        Map<String, Object> input = new HashMap<>();
        input.put("step", "");
        input.put("stepIndex", 2);
        input.put("previousResult", "analysis data");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingPreviousResult() {
        Task task = taskWith(Map.of(
                "step", "Generate recommendations",
                "stepIndex", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesNullPreviousResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("step", "Generate recommendations");
        input.put("stepIndex", 2);
        input.put("previousResult", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
        assertNotNull(result.getOutputData().get("status"));
    }

    @Test
    void outputContainsROIData() {
        Task task = taskWith(Map.of(
                "step", "Generate recommendations",
                "stepIndex", 2,
                "previousResult", "analysis"));
        TaskResult result = worker.execute(task);

        String output = (String) result.getOutputData().get("result");
        assertTrue(output.contains("ROI: capacity-planning%"));
        assertTrue(output.contains("ROI: 210%"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
