package workflowinputoutput.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculateTaxWorkerTest {

    private final CalculateTaxWorker worker = new CalculateTaxWorker();

    @Test
    void taskDefName() {
        assertEquals("calculate_tax", worker.getTaskDefName());
    }

    @Test
    void calculatesCorrectTaxAt825Percent() {
        Task task = taskWith(Map.of("discountedTotal", 100.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0825, (double) result.getOutputData().get("taxRate"), 0.0001);
        assertEquals(8.25, (double) result.getOutputData().get("taxAmount"), 0.001);
        assertEquals(108.25, (double) result.getOutputData().get("finalTotal"), 0.001);
    }

    @Test
    void calculatesForPipelineScenario() {
        // PROD-002 x2 = 699.98, SAVE20 discount = 559.984
        Task task = taskWith(Map.of("discountedTotal", 559.984));
        TaskResult result = worker.execute(task);

        double expectedTax = 559.984 * 0.0825;
        double expectedTotal = 559.984 + expectedTax;
        assertEquals(expectedTax, (double) result.getOutputData().get("taxAmount"), 0.001);
        assertEquals(expectedTotal, (double) result.getOutputData().get("finalTotal"), 0.001);
    }

    @Test
    void handlesZeroAmount() {
        Task task = taskWith(Map.of("discountedTotal", 0.0));
        TaskResult result = worker.execute(task);

        assertEquals(0.0, (double) result.getOutputData().get("taxAmount"), 0.001);
        assertEquals(0.0, (double) result.getOutputData().get("finalTotal"), 0.001);
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, (double) result.getOutputData().get("taxAmount"), 0.001);
        assertEquals(0.0, (double) result.getOutputData().get("finalTotal"), 0.001);
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("discountedTotal", 50.0));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("taxRate"));
        assertNotNull(result.getOutputData().get("taxAmount"));
        assertNotNull(result.getOutputData().get("finalTotal"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
