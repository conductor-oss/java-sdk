package chainofthought.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Step2CalculateWorkerTest {

    private final Step2CalculateWorker worker = new Step2CalculateWorker();

    @Test
    void taskDefName() {
        assertEquals("ct_step_2_calculate", worker.getTaskDefName());
    }

    @Test
    void producesCalculationFromVariables() {
        Task task = taskWith(Map.of(
                "reasoning", "Use compound interest formula: A = P(1 + r)^t where P=10000, r=0.05, t=3",
                "variables", Map.of("P", 10000, "r", 0.05, "t", 3)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("10000 * (1 + 0.05)^3", result.getOutputData().get("calculation"));
    }

    @Test
    void producesCorrectResult() {
        Task task = taskWith(Map.of(
                "reasoning", "Use compound interest formula",
                "variables", Map.of("P", 10000, "r", 0.05, "t", 3)));
        TaskResult result = worker.execute(task);

        assertEquals(11576.25, result.getOutputData().get("result"));
    }

    @Test
    void producesCorrectInterest() {
        Task task = taskWith(Map.of(
                "reasoning", "Use compound interest formula",
                "variables", Map.of("P", 10000, "r", 0.05, "t", 3)));
        TaskResult result = worker.execute(task);

        assertEquals(1576.25, result.getOutputData().get("interest"));
    }

    @Test
    void extractsVariablesFromReasoning() {
        Task task = taskWith(Map.of(
                "reasoning", "Compound interest: P=5000, r=0.1, t=2"));
        TaskResult result = worker.execute(task);

        // A = 5000 * (1 + 0.1)^2 = 5000 * 1.21 = 6050.0
        assertEquals(6050.0, result.getOutputData().get("result"));
        assertEquals(1050.0, result.getOutputData().get("interest"));
    }

    @Test
    void variablesMapOverridesReasoningText() {
        Task task = taskWith(Map.of(
                "reasoning", "P=5000, r=0.1, t=2",
                "variables", Map.of("P", 10000, "r", 0.05, "t", 3)));
        TaskResult result = worker.execute(task);

        // Variables map should take precedence
        assertEquals(11576.25, result.getOutputData().get("result"));
    }

    @Test
    void handlesNullReasoning() {
        Map<String, Object> input = new HashMap<>();
        input.put("reasoning", null);
        input.put("variables", Map.of("P", 10000));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // Uses defaults for missing vars: r=0.05, t=3
        assertEquals(11576.25, result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // All defaults: P=10000, r=0.05, t=3
        assertEquals(11576.25, result.getOutputData().get("result"));
        assertEquals(1576.25, result.getOutputData().get("interest"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("reasoning", "test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("calculation"));
        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("interest"));
    }

    @Test
    void resultAndInterestAreDoubles() {
        Task task = taskWith(Map.of("reasoning", "test"));
        TaskResult result = worker.execute(task);

        assertInstanceOf(Double.class, result.getOutputData().get("result"));
        assertInstanceOf(Double.class, result.getOutputData().get("interest"));
    }

    @Test
    void differentVariablesProduceDifferentResults() {
        Task task1 = taskWith(Map.of("variables", Map.of("P", 10000, "r", 0.05, "t", 3)));
        Task task2 = taskWith(Map.of("variables", Map.of("P", 20000, "r", 0.10, "t", 5)));

        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertNotEquals(r1.getOutputData().get("result"), r2.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
