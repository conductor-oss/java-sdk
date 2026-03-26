package sensordataprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectReadingsWorkerTest {

    private final CollectReadingsWorker worker = new CollectReadingsWorker();

    @Test
    void taskDefName() {
        assertEquals("sen_collect_readings", worker.getTaskDefName());
    }

    @Test
    void collectsReadingsSuccessfully() {
        Task task = taskWith(Map.of("batchId", "BATCH-001", "sensorGroupId", "SG-A", "timeWindowMinutes", 60));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1200, result.getOutputData().get("readingCount"));
    }

    @Test
    void outputContainsReadings() {
        Task task = taskWith(Map.of("sensorGroupId", "SG-B"));
        TaskResult result = worker.execute(task);

        Object readings = result.getOutputData().get("readings");
        assertNotNull(readings);
        assertInstanceOf(List.class, readings);
        assertEquals(3, ((List<?>) readings).size());
    }

    @Test
    void outputContainsSensorCount() {
        Task task = taskWith(Map.of("sensorGroupId", "SG-C"));
        TaskResult result = worker.execute(task);

        assertEquals(50, result.getOutputData().get("sensorCount"));
    }

    @Test
    void readingsContainExpectedFields() {
        Task task = taskWith(Map.of("sensorGroupId", "SG-D"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> readings = (List<Map<String, Object>>) result.getOutputData().get("readings");
        Map<String, Object> first = readings.get(0);
        assertEquals("S-001", first.get("sensorId"));
        assertEquals(72.5, first.get("temperature"));
        assertEquals(45, first.get("humidity"));
    }

    @Test
    void handlesNullSensorGroupId() {
        Map<String, Object> input = new HashMap<>();
        input.put("sensorGroupId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1200, result.getOutputData().get("readingCount"));
    }

    @Test
    void thirdReadingHasHighTemperature() {
        Task task = taskWith(Map.of("sensorGroupId", "SG-E"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> readings = (List<Map<String, Object>>) result.getOutputData().get("readings");
        assertEquals(89.2, readings.get(2).get("temperature"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
