package chainofthought.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Step1ReasonWorkerTest {

    private final Step1ReasonWorker worker = new Step1ReasonWorker();

    @Test
    void taskDefName() {
        assertEquals("ct_step_1_reason", worker.getTaskDefName());
    }

    @Test
    void producesReasoning() {
        Task task = taskWith(Map.of(
                "problem", "What is the compound interest on $10,000 at 5% annual rate for 3 years?",
                "understanding", "Calculate compound interest on a $10,000 principal at 5% annual rate for 3 years"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Use compound interest formula: A = P(1 + r)^t where P=10000, r=0.05, t=3",
                result.getOutputData().get("reasoning"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void producesVariables() {
        Task task = taskWith(Map.of(
                "problem", "test",
                "understanding", "test understanding"));
        TaskResult result = worker.execute(task);

        Map<String, Object> variables = (Map<String, Object>) result.getOutputData().get("variables");
        assertNotNull(variables);
        assertEquals(10000, variables.get("P"));
        assertEquals(0.05, variables.get("r"));
        assertEquals(3, variables.get("t"));
    }

    @Test
    void handlesNullProblem() {
        Map<String, Object> input = new HashMap<>();
        input.put("problem", null);
        input.put("understanding", "test understanding");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("reasoning"));
    }

    @Test
    void handlesNullUnderstanding() {
        Map<String, Object> input = new HashMap<>();
        input.put("problem", "test problem");
        input.put("understanding", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("reasoning"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("reasoning"));
        assertNotNull(result.getOutputData().get("variables"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("problem", "test", "understanding", "test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("reasoning"));
        assertTrue(result.getOutputData().containsKey("variables"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void variablesHaveCorrectTypes() {
        Task task = taskWith(Map.of("problem", "test", "understanding", "test"));
        TaskResult result = worker.execute(task);

        Map<String, Object> variables = (Map<String, Object>) result.getOutputData().get("variables");
        assertInstanceOf(Integer.class, variables.get("P"));
        assertInstanceOf(Double.class, variables.get("r"));
        assertInstanceOf(Integer.class, variables.get("t"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
