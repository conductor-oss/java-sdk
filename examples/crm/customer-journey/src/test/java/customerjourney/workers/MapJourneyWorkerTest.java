package customerjourney.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapJourneyWorkerTest {

    private final MapJourneyWorker worker = new MapJourneyWorker();

    @Test
    void taskDefName() {
        assertEquals("cjy_map_journey", worker.getTaskDefName());
    }

    @Test
    void returnsJourneyMap() {
        Task task = taskWith(Map.of("touchpoints", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("journeyMap"));
    }

    @Test
    void returnsStageCount() {
        Task task = taskWith(Map.of("touchpoints", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("stageCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void journeyMapContainsExpectedStages() {
        Task task = taskWith(Map.of("touchpoints", List.of()));
        TaskResult result = worker.execute(task);

        Map<String, Integer> map = (Map<String, Integer>) result.getOutputData().get("journeyMap");
        assertTrue(map.containsKey("awareness"));
        assertTrue(map.containsKey("purchase"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
