package predictivemonitoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TrainModelTest {

    private final TrainModel worker = new TrainModel();

    @Test
    void taskDefName() {
        assertEquals("pdm_train_model", worker.getTaskDefName());
    }

    @Test
    void returnsModelMetadata() {
        Task task = taskWith(43200);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("pdm-model-20260308", result.getOutputData().get("modelId"));
        assertEquals(94.2, result.getOutputData().get("accuracy"));
        assertEquals("prophet", result.getOutputData().get("algorithm"));
        assertEquals(8500, result.getOutputData().get("trainingTimeMs"));
    }

    @Test
    void completesWithNullDataPoints() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("modelId"));
        assertNotNull(result.getOutputData().get("accuracy"));
    }

    @Test
    void accuracyIsNumeric() {
        Task task = taskWith(1000);

        TaskResult result = worker.execute(task);

        Object accuracy = result.getOutputData().get("accuracy");
        assertInstanceOf(Number.class, accuracy);
        assertTrue(((Number) accuracy).doubleValue() > 0);
    }

    private Task taskWith(int dataPoints) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("dataPoints", dataPoints);
        task.setInputData(input);
        return task;
    }
}
