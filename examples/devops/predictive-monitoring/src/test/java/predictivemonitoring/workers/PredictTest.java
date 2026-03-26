package predictivemonitoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PredictTest {

    private final Predict worker = new Predict();

    @Test
    void taskDefName() {
        assertEquals("pdm_predict", worker.getTaskDefName());
    }

    @Test
    void returnsPredictionFields() {
        Task task = taskWith(24, 94.2);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(88.5, result.getOutputData().get("predictedPeak"));
        assertNotNull(result.getOutputData().get("predictedPeakTime"));
        assertEquals(72.3, result.getOutputData().get("breachLikelihood"));
    }

    @Test
    void confidenceIntervalContainsLowAndHigh() {
        Task task = taskWith(12, 90.0);

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> ci = (Map<String, Object>) result.getOutputData().get("confidenceInterval");
        assertNotNull(ci);
        assertEquals(78.2, ci.get("low"));
        assertEquals(95.1, ci.get("high"));
        assertTrue(((Number) ci.get("low")).doubleValue() < ((Number) ci.get("high")).doubleValue());
    }

    @Test
    void completesWithMissingInputs() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("predictedPeak"));
        assertNotNull(result.getOutputData().get("breachLikelihood"));
    }

    private Task taskWith(int forecastHours, double modelAccuracy) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("forecastHours", forecastHours);
        input.put("modelAccuracy", modelAccuracy);
        task.setInputData(input);
        return task;
    }
}
