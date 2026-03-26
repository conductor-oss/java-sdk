package goaldecomposition.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Subgoal1WorkerTest {

    private final Subgoal1Worker worker = new Subgoal1Worker();

    @Test
    void taskDefName() {
        assertEquals("gd_subgoal_1", worker.getTaskDefName());
    }

    @Test
    void executesSubgoalSuccessfully() {
        Task task = taskWith(Map.of(
                "subgoal", "Analyze current system performance bottlenecks",
                "index", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsBottleneckAnalysisResult() {
        Task task = taskWith(Map.of(
                "subgoal", "Analyze current system performance bottlenecks",
                "index", 0));
        TaskResult result = worker.execute(task);

        String resultStr = (String) result.getOutputData().get("result");
        assertEquals("Identified 3 bottlenecks: database queries (40%), API serialization (25%), network latency (15%)",
                resultStr);
    }

    @Test
    void returnsCompleteStatus() {
        Task task = taskWith(Map.of(
                "subgoal", "Analyze current system performance bottlenecks",
                "index", 0));
        TaskResult result = worker.execute(task);

        assertEquals("complete", result.getOutputData().get("status"));
    }

    @Test
    void resultContainsDatabaseQueryBottleneck() {
        Task task = taskWith(Map.of(
                "subgoal", "Analyze current system performance bottlenecks",
                "index", 0));
        TaskResult result = worker.execute(task);

        String resultStr = (String) result.getOutputData().get("result");
        assertTrue(resultStr.contains("database queries (40%)"));
    }

    @Test
    void handlesEmptySubgoal() {
        Map<String, Object> input = new HashMap<>();
        input.put("subgoal", "");
        input.put("index", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesNullSubgoal() {
        Map<String, Object> input = new HashMap<>();
        input.put("subgoal", null);
        input.put("index", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
        assertNotNull(result.getOutputData().get("status"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
