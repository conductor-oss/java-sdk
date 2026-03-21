package compliancereporting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapControlsWorkerTest {

    private final MapControlsWorker worker = new MapControlsWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_map_controls", worker.getTaskDefName());
    }

    @Test
    void mapsControlsWithEvidenceData() {
        Task task = taskWith(Map.of("map_controlsData", Map.of("collect_evidenceId", "CE-1")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("map_controls"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void mapsControlsWithEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("map_controls"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("map_controlsData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of("map_controlsData", "data"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("map_controls"));
        assertTrue(result.getOutputData().containsKey("processed"));
    }

    @Test
    void processedIsTrue() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("map_controlsData", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void mapControlsValueIsTrue() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("map_controls"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
