package chainofthought.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Step3VerifyWorkerTest {

    private final Step3VerifyWorker worker = new Step3VerifyWorker();

    @Test
    void taskDefName() {
        assertEquals("ct_step_3_verify", worker.getTaskDefName());
    }

    @Test
    void verifiesCorrectResult() {
        Task task = taskWith(Map.of(
                "calculation", "10000 * (1 + 0.05)^3",
                "result", 11576.25));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(11576.25, result.getOutputData().get("verifiedResult"));
    }

    @Test
    void producesVerifiedFlag() {
        Task task = taskWith(Map.of(
                "calculation", "10000 * (1 + 0.05)^3",
                "result", 11576.25));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("verified"));
    }

    @Test
    void producesHighConfidenceForCorrectResult() {
        Task task = taskWith(Map.of(
                "calculation", "10000 * (1 + 0.05)^3",
                "result", 11576.25));
        TaskResult result = worker.execute(task);

        assertEquals(1.0, result.getOutputData().get("confidence"));
    }

    @Test
    void detectsIncorrectResult() {
        Task task = taskWith(Map.of(
                "calculation", "10000 * (1 + 0.05)^3",
                "result", 99999.0));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("verified"));
        double confidence = ((Number) result.getOutputData().get("confidence")).doubleValue();
        assertTrue(confidence < 1.0);
    }

    @Test
    void handlesNullCalculation() {
        Map<String, Object> input = new HashMap<>();
        input.put("calculation", null);
        input.put("result", 11576.25);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // When calculation can't be parsed, trusts the input
        assertEquals(11576.25, result.getOutputData().get("verifiedResult"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("verifiedResult"));
        assertNotNull(result.getOutputData().get("verified"));
        assertNotNull(result.getOutputData().get("confidence"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("calculation", "test", "result", 0));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("verifiedResult"));
        assertTrue(result.getOutputData().containsKey("verified"));
        assertTrue(result.getOutputData().containsKey("confidence"));
    }

    @Test
    void confidenceIsDouble() {
        Task task = taskWith(Map.of("calculation", "test", "result", 0));
        TaskResult result = worker.execute(task);

        assertInstanceOf(Double.class, result.getOutputData().get("confidence"));
    }

    @Test
    void verifiesDifferentCalculation() {
        Task task = taskWith(Map.of(
                "calculation", "5000 * (1 + 0.1)^2",
                "result", 6050.0));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("verified"));
        assertEquals(6050.0, result.getOutputData().get("verifiedResult"));
        assertEquals(1.0, result.getOutputData().get("confidence"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
