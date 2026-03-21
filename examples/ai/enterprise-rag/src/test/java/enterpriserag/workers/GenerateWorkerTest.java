package enterpriserag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateWorkerTest {

    private final GenerateWorker worker = new GenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("er_generate", worker.getTaskDefName());
    }

    @Test
    void returnsGeneratedAnswer() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", "What is RAG?");
        input.put("context", "some context");
        input.put("tokenBudget", 500);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
        assertTrue(((String) result.getOutputData().get("answer")).length() > 0);
    }

    @Test
    void returnsModelMetadata() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", "What is RAG?");
        input.put("context", "some context");
        input.put("tokenBudget", 500);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(187, result.getOutputData().get("tokensUsed"));
        assertEquals("gpt-4o", result.getOutputData().get("model"));
        assertEquals(820, result.getOutputData().get("latencyMs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
