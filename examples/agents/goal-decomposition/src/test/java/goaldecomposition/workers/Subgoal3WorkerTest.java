package goaldecomposition.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Subgoal3WorkerTest {

    private final Subgoal3Worker worker = new Subgoal3Worker();

    @Test
    void taskDefName() {
        assertEquals("gd_subgoal_3", worker.getTaskDefName());
    }

    @Test
    void executesSubgoalSuccessfully() {
        Task task = taskWith(Map.of(
                "subgoal", "Evaluate infrastructure scaling options",
                "index", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsScalingResult() {
        Task task = taskWith(Map.of(
                "subgoal", "Evaluate infrastructure scaling options",
                "index", 2));
        TaskResult result = worker.execute(task);

        String resultStr = (String) result.getOutputData().get("result");
        assertEquals("Horizontal pod autoscaling + read replicas can handle 3x current load at 20% cost increase",
                resultStr);
    }

    @Test
    void returnsCompleteStatus() {
        Task task = taskWith(Map.of(
                "subgoal", "Evaluate infrastructure scaling options",
                "index", 2));
        TaskResult result = worker.execute(task);

        assertEquals("complete", result.getOutputData().get("status"));
    }

    @Test
    void resultContainsAutoscaling() {
        Task task = taskWith(Map.of(
                "subgoal", "Evaluate infrastructure scaling options",
                "index", 2));
        TaskResult result = worker.execute(task);

        String resultStr = (String) result.getOutputData().get("result");
        assertTrue(resultStr.contains("Horizontal pod autoscaling"));
    }

    @Test
    void handlesEmptySubgoal() {
        Map<String, Object> input = new HashMap<>();
        input.put("subgoal", "");
        input.put("index", 2);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesNullSubgoal() {
        Map<String, Object> input = new HashMap<>();
        input.put("subgoal", null);
        input.put("index", 2);
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
