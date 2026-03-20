package calculatoragent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComputeStepsWorkerTest {

    private final ComputeStepsWorker worker = new ComputeStepsWorker();

    @Test
    void taskDefName() {
        assertEquals("ca_compute_steps", worker.getTaskDefName());
    }

    @Test
    void happyPathReturnsFourSteps() {
        Task task = taskWith(Map.of(
                "tokens", List.of(Map.of("type", "number", "value", "15")),
                "operationOrder", List.of("step1", "step2", "step3", "step4"),
                "precision", "exact"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) result.getOutputData().get("steps");
        assertEquals(4, steps.size());
    }

    @Test
    void finalResultIs176() {
        Task task = taskWith(Map.of(
                "tokens", List.of(),
                "operationOrder", List.of(),
                "precision", "exact"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(176, result.getOutputData().get("finalResult"));
    }

    @Test
    void stepsHaveCorrectStructure() {
        Task task = taskWith(Map.of(
                "tokens", List.of(),
                "operationOrder", List.of(),
                "precision", "exact"
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) result.getOutputData().get("steps");
        Map<String, Object> firstStep = steps.get(0);

        assertEquals(1, firstStep.get("step"));
        assertEquals("8+4=12", firstStep.get("operation"));
        assertEquals(12, firstStep.get("intermediate"));
        assertEquals("Evaluate parentheses first", firstStep.get("rule"));
    }

    @Test
    void stepsFollowPemdas() {
        Task task = taskWith(Map.of(
                "tokens", List.of(),
                "operationOrder", List.of(),
                "precision", "exact"
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) result.getOutputData().get("steps");

        // Step 1: parentheses, Step 2: multiplication, Step 3: division, Step 4: subtraction
        assertEquals(12, steps.get(0).get("intermediate"));
        assertEquals(180, steps.get(1).get("intermediate"));
        assertEquals(4, steps.get(2).get("intermediate"));
        assertEquals(176, steps.get(3).get("intermediate"));
    }

    @Test
    void precisionPassedThrough() {
        Task task = taskWith(Map.of(
                "tokens", List.of(),
                "operationOrder", List.of(),
                "precision", "decimal"
        ));
        TaskResult result = worker.execute(task);

        assertEquals("decimal", result.getOutputData().get("precision"));
    }

    @Test
    void defaultPrecisionWhenNull() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("exact", result.getOutputData().get("precision"));
    }

    @Test
    void defaultPrecisionWhenBlank() {
        Map<String, Object> input = new HashMap<>();
        input.put("precision", "  ");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("exact", result.getOutputData().get("precision"));
    }

    @Test
    void stepNumbersAreSequential() {
        Task task = taskWith(Map.of(
                "tokens", List.of(),
                "operationOrder", List.of(),
                "precision", "exact"
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) result.getOutputData().get("steps");
        for (int i = 0; i < steps.size(); i++) {
            assertEquals(i + 1, steps.get(i).get("step"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
