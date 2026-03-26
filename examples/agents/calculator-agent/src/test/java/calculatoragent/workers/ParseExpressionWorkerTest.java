package calculatoragent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseExpressionWorkerTest {

    private final ParseExpressionWorker worker = new ParseExpressionWorker();

    @Test
    void taskDefName() {
        assertEquals("ca_parse_expression", worker.getTaskDefName());
    }

    @Test
    void happyPathReturnsTokensAndOrder() {
        Task task = taskWith(Map.of("expression", "15 * (8 + 4) - 12 / 3", "precision", "exact"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("tokens"));
        assertTrue(result.getOutputData().get("tokens") instanceof List);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tokens = (List<Map<String, Object>>) result.getOutputData().get("tokens");
        assertFalse(tokens.isEmpty());
    }

    @Test
    void returnsOperationOrder() {
        Task task = taskWith(Map.of("expression", "15 * (8 + 4) - 12 / 3", "precision", "exact"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("operationOrder"));
        @SuppressWarnings("unchecked")
        List<String> order = (List<String>) result.getOutputData().get("operationOrder");
        assertEquals(4, order.size());
    }

    @Test
    void returnsValidFlag() {
        Task task = taskWith(Map.of("expression", "15 * (8 + 4) - 12 / 3", "precision", "exact"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("valid"));
        assertEquals("arithmetic", result.getOutputData().get("expressionType"));
    }

    @Test
    void precisionPassedThrough() {
        Task task = taskWith(Map.of("expression", "1 + 2", "precision", "decimal"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("decimal", result.getOutputData().get("precision"));
    }

    @Test
    void defaultsForNullExpression() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("exact", result.getOutputData().get("precision"));
    }

    @Test
    void defaultsForBlankExpression() {
        Map<String, Object> input = new HashMap<>();
        input.put("expression", "   ");
        input.put("precision", "");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("exact", result.getOutputData().get("precision"));
    }

    @Test
    void tokensContainExpectedTypes() {
        Task task = taskWith(Map.of("expression", "15 * (8 + 4) - 12 / 3", "precision", "exact"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tokens = (List<Map<String, Object>>) result.getOutputData().get("tokens");

        // Verify tokens include numbers, operators, and parentheses
        boolean hasNumber = tokens.stream().anyMatch(t -> "number".equals(t.get("type")));
        boolean hasOperator = tokens.stream().anyMatch(t -> "operator".equals(t.get("type")));
        boolean hasParenOpen = tokens.stream().anyMatch(t -> "parenthesis_open".equals(t.get("type")));
        boolean hasParenClose = tokens.stream().anyMatch(t -> "parenthesis_close".equals(t.get("type")));

        assertTrue(hasNumber);
        assertTrue(hasOperator);
        assertTrue(hasParenOpen);
        assertTrue(hasParenClose);
    }

    @Test
    void operationOrderMentionsParenthesesFirst() {
        Task task = taskWith(Map.of("expression", "15 * (8 + 4) - 12 / 3", "precision", "exact"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> order = (List<String>) result.getOutputData().get("operationOrder");
        assertTrue(order.get(0).toLowerCase().contains("parenthes"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
