package toolaugmentedgeneration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CallToolWorkerTest {

    private final CallToolWorker worker = new CallToolWorker();

    @Test
    void taskDefName() {
        assertEquals("tg_call_tool", worker.getTaskDefName());
    }

    @Test
    void returnsToolResult() {
        Task task = taskWith(Map.of(
                "toolName", "version_lookup",
                "toolQuery", "Node.js current LTS version"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String toolResult = (String) result.getOutputData().get("toolResult");
        assertNotNull(toolResult);
        assertTrue(toolResult.contains("v22.x"));
        assertTrue(toolResult.contains("Jod"));
    }

    @Test
    void returnsSource() {
        Task task = taskWith(Map.of(
                "toolName", "version_lookup",
                "toolQuery", "Node.js current LTS version"));
        TaskResult result = worker.execute(task);

        assertEquals("nodejs.org", result.getOutputData().get("source"));
    }

    @Test
    void returnsCachedFlag() {
        Task task = taskWith(Map.of(
                "toolName", "version_lookup",
                "toolQuery", "Node.js current LTS version"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("cached"));
    }

    @Test
    void returnsAllOutputFields() {
        Task task = taskWith(Map.of(
                "toolName", "version_lookup",
                "toolQuery", "Node.js current LTS version"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("toolResult"));
        assertNotNull(result.getOutputData().get("source"));
        assertNotNull(result.getOutputData().get("cached"));
    }

    @Test
    void handlesEmptyToolName() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", "");
        input.put("toolQuery", "Node.js current LTS version");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("toolResult"));
    }

    @Test
    void handlesNullToolQuery() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", "version_lookup");
        input.put("toolQuery", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("toolResult"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("toolResult"));
        assertNotNull(result.getOutputData().get("source"));
        assertNotNull(result.getOutputData().get("cached"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
