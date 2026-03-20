package toolaugmentedgeneration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectGapWorkerTest {

    private final DetectGapWorker worker = new DetectGapWorker();

    @Test
    void taskDefName() {
        assertEquals("tg_detect_gap", worker.getTaskDefName());
    }

    @Test
    void detectsToolName() {
        Task task = taskWith(Map.of(
                "partialText", "Node.js was created by Ryan Dahl and first released in 2009. Its current LTS version is",
                "gapType", "factual_lookup"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("version_lookup", result.getOutputData().get("toolName"));
    }

    @Test
    void detectsToolQuery() {
        Task task = taskWith(Map.of(
                "partialText", "Node.js was created by Ryan Dahl",
                "gapType", "factual_lookup"));
        TaskResult result = worker.execute(task);

        assertEquals("Node.js current LTS version", result.getOutputData().get("toolQuery"));
    }

    @Test
    void detectsGapLocation() {
        Task task = taskWith(Map.of(
                "partialText", "Node.js was created by Ryan Dahl",
                "gapType", "factual_lookup"));
        TaskResult result = worker.execute(task);

        assertEquals("end", result.getOutputData().get("gapLocation"));
    }

    @Test
    void returnsAllOutputFields() {
        Task task = taskWith(Map.of(
                "partialText", "Some text",
                "gapType", "factual_lookup"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("toolName"));
        assertNotNull(result.getOutputData().get("toolQuery"));
        assertNotNull(result.getOutputData().get("gapLocation"));
    }

    @Test
    void handlesEmptyPartialText() {
        Map<String, Object> input = new HashMap<>();
        input.put("partialText", "");
        input.put("gapType", "factual_lookup");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("toolName"));
    }

    @Test
    void handlesNullGapType() {
        Map<String, Object> input = new HashMap<>();
        input.put("partialText", "Some partial text");
        input.put("gapType", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("toolName"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("toolName"));
        assertNotNull(result.getOutputData().get("toolQuery"));
        assertNotNull(result.getOutputData().get("gapLocation"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
