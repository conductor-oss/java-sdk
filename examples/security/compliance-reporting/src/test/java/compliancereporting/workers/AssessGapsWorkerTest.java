package compliancereporting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssessGapsWorkerTest {

    private final AssessGapsWorker worker = new AssessGapsWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_assess_gaps", worker.getTaskDefName());
    }

    @Test
    void assessesGapsWithControlData() {
        Task task = taskWith(Map.of("assess_gapsData", Map.of("map_controls", true)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("assess_gaps"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void assessesGapsWithEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("assess_gaps"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("assess_gapsData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of("assess_gapsData", "data"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("assess_gaps"));
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
        Task task = taskWith(Map.of("assess_gapsData", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void assessGapsValueIsTrue() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("assess_gaps"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
