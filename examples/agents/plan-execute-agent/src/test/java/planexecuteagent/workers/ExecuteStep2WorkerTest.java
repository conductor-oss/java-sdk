package planexecuteagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteStep2WorkerTest {

    private final ExecuteStep2Worker worker = new ExecuteStep2Worker();

    @Test
    void taskDefName() {
        assertEquals("pe_execute_step_2", worker.getTaskDefName());
    }

    @Test
    void executesStepSuccessfully() {
        Task task = taskWith(Map.of(
                "step", "Analyze trends and identify opportunities",
                "stepIndex", 1,
                "previousResult", "Collected data on 5 competitors; market size estimated at $4.2B"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsOpportunitiesResult() {
        Task task = taskWith(Map.of(
                "step", "Analyze trends and identify opportunities",
                "stepIndex", 1,
                "previousResult", "Collected data on 5 competitors"));
        TaskResult result = worker.execute(task);

        String output = (String) result.getOutputData().get("result");
        assertEquals("Identified 3 growth opportunities: API platform, enterprise tier, international expansion", output);
    }

    @Test
    void returnsCompleteStatus() {
        Task task = taskWith(Map.of(
                "step", "Analyze trends",
                "stepIndex", 1,
                "previousResult", "some data"));
        TaskResult result = worker.execute(task);

        assertEquals("complete", result.getOutputData().get("status"));
    }

    @Test
    void handlesEmptyStep() {
        Map<String, Object> input = new HashMap<>();
        input.put("step", "");
        input.put("stepIndex", 1);
        input.put("previousResult", "some data");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingPreviousResult() {
        Task task = taskWith(Map.of(
                "step", "Analyze trends",
                "stepIndex", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesNullPreviousResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("step", "Analyze trends");
        input.put("stepIndex", 1);
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
    void outputContainsGrowthOpportunities() {
        Task task = taskWith(Map.of(
                "step", "Analyze trends",
                "stepIndex", 1,
                "previousResult", "market data"));
        TaskResult result = worker.execute(task);

        String output = (String) result.getOutputData().get("result");
        assertTrue(output.contains("API platform"));
        assertTrue(output.contains("enterprise tier"));
        assertTrue(output.contains("international expansion"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
