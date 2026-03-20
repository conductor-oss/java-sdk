package tooluseconditional.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorWorkerTest {

    private final CalculatorWorker worker = new CalculatorWorker();

    @Test
    void taskDefName() {
        assertEquals("tc_calculator", worker.getTaskDefName());
    }

    @Test
    void handlesSqrtExpression() {
        Task task = taskWith(Map.of("expression", "the square root of 144", "userQuery", "Calculate the square root of 144"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("12"));

        @SuppressWarnings("unchecked")
        Map<String, Object> calculation = (Map<String, Object>) result.getOutputData().get("calculation");
        assertEquals(12, calculation.get("result"));
        assertEquals("square_root", calculation.get("method"));
    }

    @Test
    void handlesFactorialExpression() {
        Task task = taskWith(Map.of("expression", "factorial of 5", "userQuery", "compute factorial of 5"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("120"));

        @SuppressWarnings("unchecked")
        Map<String, Object> calculation = (Map<String, Object>) result.getOutputData().get("calculation");
        assertEquals(120, calculation.get("result"));
        assertEquals("factorial", calculation.get("method"));
    }

    @Test
    void handlesArithmeticExpression() {
        Task task = taskWith(Map.of("expression", "7 * 6", "userQuery", "calculate 7 * 6"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("42"));

        @SuppressWarnings("unchecked")
        Map<String, Object> calculation = (Map<String, Object>) result.getOutputData().get("calculation");
        assertEquals("arithmetic", calculation.get("method"));
    }

    @Test
    void returnsToolUsedCalculator() {
        Task task = taskWith(Map.of("expression", "2 + 2"));
        TaskResult result = worker.execute(task);

        assertEquals("calculator", result.getOutputData().get("toolUsed"));
    }

    @Test
    void calculationContainsSteps() {
        Task task = taskWith(Map.of("expression", "sqrt of 144"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> calculation = (Map<String, Object>) result.getOutputData().get("calculation");
        @SuppressWarnings("unchecked")
        List<String> steps = (List<String>) calculation.get("steps");
        assertNotNull(steps);
        assertFalse(steps.isEmpty());
    }

    @Test
    void handlesNullExpression() {
        Map<String, Object> input = new HashMap<>();
        input.put("expression", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("calculation"));
    }

    @Test
    void answerContainsExpression() {
        Task task = taskWith(Map.of("expression", "the square root of 144"));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("the square root of 144"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
