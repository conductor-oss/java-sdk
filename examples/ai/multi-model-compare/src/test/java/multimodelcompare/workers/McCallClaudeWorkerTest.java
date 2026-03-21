package multimodelcompare.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McCallClaudeWorkerTest {

    private final McCallClaudeWorker worker = new McCallClaudeWorker();

    @Test
    void taskDefName() {
        assertEquals("mc_call_claude", worker.getTaskDefName());
    }

    @Test
    void returnsClaudeResponse() {
        Task task = taskWith(new HashMap<>(Map.of("prompt", "What is Conductor?")));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("claude-3", result.getOutputData().get("model"));
        assertEquals("Conductor is a workflow engine that brings durability and observability to distributed systems and AI pipelines.",
                result.getOutputData().get("response"));
        assertEquals(980, result.getOutputData().get("latencyMs"));
        assertEquals(92, result.getOutputData().get("tokens"));
        assertEquals(0.0069, result.getOutputData().get("cost"));
        assertEquals(9.0, result.getOutputData().get("quality"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(new HashMap<>(Map.of("prompt", "test")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("model"));
        assertTrue(result.getOutputData().containsKey("response"));
        assertTrue(result.getOutputData().containsKey("latencyMs"));
        assertTrue(result.getOutputData().containsKey("tokens"));
        assertTrue(result.getOutputData().containsKey("cost"));
        assertTrue(result.getOutputData().containsKey("quality"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
