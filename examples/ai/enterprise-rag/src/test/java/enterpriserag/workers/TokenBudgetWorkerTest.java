package enterpriserag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TokenBudgetWorkerTest {

    private final TokenBudgetWorker worker = new TokenBudgetWorker();

    @Test
    void taskDefName() {
        assertEquals("er_token_budget", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsTokenBudgetDetails() {
        List<Map<String, Object>> context = List.of(
                Map.of("id", "doc-1", "text", "Some text", "tokens", 100),
                Map.of("id", "doc-2", "text", "More text", "tokens", 200)
        );

        Map<String, Object> input = new HashMap<>();
        input.put("context", context);
        input.put("userId", "user-42");

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(38000, result.getOutputData().get("remainingBudget"));
        assertEquals(300, result.getOutputData().get("contextTokens"));

        List<Map<String, Object>> trimmed =
                (List<Map<String, Object>>) result.getOutputData().get("trimmedContext");
        assertNotNull(trimmed);
        assertEquals(2, trimmed.size());
    }

    @Test
    void handlesNullContext() {
        Map<String, Object> input = new HashMap<>();
        input.put("context", null);
        input.put("userId", "user-1");

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("contextTokens"));
        assertEquals(38000, result.getOutputData().get("remainingBudget"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
