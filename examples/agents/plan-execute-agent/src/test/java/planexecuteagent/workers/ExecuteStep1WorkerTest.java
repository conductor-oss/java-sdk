package planexecuteagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteStep1WorkerTest {

    private final ExecuteStep1Worker worker = new ExecuteStep1Worker();

    @Test
    void taskDefName() {
        assertEquals("pe_execute_step_1", worker.getTaskDefName());
    }

    @Test
    void executesStepSuccessfully() {
        Task task = taskWith(Map.of(
                "step", "Gather market data and competitor analysis",
                "stepIndex", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsMarketDataResult() {
        Task task = taskWith(Map.of(
                "step", "Gather market data and competitor analysis",
                "stepIndex", 0));
        TaskResult result = worker.execute(task);

        String output = (String) result.getOutputData().get("result");
        assertEquals("Collected data on 5 competitors; market size estimated at $4.2B", output);
    }

    @Test
    void returnsCompleteStatus() {
        Task task = taskWith(Map.of(
                "step", "Gather market data and competitor analysis",
                "stepIndex", 0));
        TaskResult result = worker.execute(task);

        assertEquals("complete", result.getOutputData().get("status"));
    }

    @Test
    void handlesEmptyStep() {
        Map<String, Object> input = new HashMap<>();
        input.put("step", "");
        input.put("stepIndex", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingStep() {
        Task task = taskWith(Map.of("stepIndex", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesNullStep() {
        Map<String, Object> input = new HashMap<>();
        input.put("step", null);
        input.put("stepIndex", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingStepIndex() {
        Task task = taskWith(Map.of("step", "Gather market data"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void outputContainsCompetitorData() {
        Task task = taskWith(Map.of(
                "step", "Gather market data",
                "stepIndex", 0));
        TaskResult result = worker.execute(task);

        String output = (String) result.getOutputData().get("result");
        assertTrue(output.contains("5 competitors"));
        assertTrue(output.contains("$4.2B"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
