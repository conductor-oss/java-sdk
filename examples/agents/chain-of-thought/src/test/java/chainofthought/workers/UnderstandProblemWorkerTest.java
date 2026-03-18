package chainofthought.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UnderstandProblemWorkerTest {

    private final UnderstandProblemWorker worker = new UnderstandProblemWorker();

    @Test
    void taskDefName() {
        assertEquals("ct_understand_problem", worker.getTaskDefName());
    }

    @Test
    void producesUnderstanding() {
        Task task = taskWith(Map.of(
                "problem", "What is the compound interest on $10,000 at 5% annual rate for 3 years?"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Calculate compound interest on a $10,000 principal at 5% annual rate for 3 years",
                result.getOutputData().get("understanding"));
    }

    @Test
    void producesType() {
        Task task = taskWith(Map.of(
                "problem", "What is the compound interest on $10,000 at 5% annual rate for 3 years?"));
        TaskResult result = worker.execute(task);

        assertEquals("financial_calculation", result.getOutputData().get("type"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void producesKnownValues() {
        Task task = taskWith(Map.of(
                "problem", "What is the compound interest on $10,000 at 5% annual rate for 3 years?"));
        TaskResult result = worker.execute(task);

        Map<String, Object> knownValues = (Map<String, Object>) result.getOutputData().get("knownValues");
        assertNotNull(knownValues);
        assertEquals(10000, knownValues.get("principal"));
        assertEquals(0.05, knownValues.get("rate"));
        assertEquals(3, knownValues.get("years"));
    }

    @Test
    void handlesEmptyProblem() {
        Map<String, Object> input = new HashMap<>();
        input.put("problem", "");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("understanding"));
    }

    @Test
    void handlesNullProblem() {
        Map<String, Object> input = new HashMap<>();
        input.put("problem", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("understanding"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("understanding"));
        assertNotNull(result.getOutputData().get("type"));
        assertNotNull(result.getOutputData().get("knownValues"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("problem", "test problem"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("understanding"));
        assertTrue(result.getOutputData().containsKey("type"));
        assertTrue(result.getOutputData().containsKey("knownValues"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
