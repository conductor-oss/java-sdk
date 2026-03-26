package customerjourney.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeWorkerTest {

    private final AnalyzeWorker worker = new AnalyzeWorker();

    @Test
    void taskDefName() {
        assertEquals("cjy_analyze", worker.getTaskDefName());
    }

    @Test
    void returnsInsights() {
        Task task = taskWith(Map.of("journeyMap", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("insights"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void insightsContainExpectedKeys() {
        Task task = taskWith(Map.of("journeyMap", Map.of()));
        TaskResult result = worker.execute(task);

        Map<String, Object> insights = (Map<String, Object>) result.getOutputData().get("insights");
        assertTrue(insights.containsKey("avgConversionDays"));
        assertTrue(insights.containsKey("dropOffStage"));
        assertTrue(insights.containsKey("topChannel"));
        assertTrue(insights.containsKey("conversionRate"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
