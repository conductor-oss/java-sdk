package goaldecomposition.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DecomposeGoalWorkerTest {

    private final DecomposeGoalWorker worker = new DecomposeGoalWorker();

    @Test
    void taskDefName() {
        assertEquals("gd_decompose_goal", worker.getTaskDefName());
    }

    @Test
    void decomposesGoalIntoThreeSubgoals() {
        Task task = taskWith(Map.of("goal", "Improve application performance by 3x"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<String> subgoals = (List<String>) result.getOutputData().get("subgoals");
        assertNotNull(subgoals);
        assertEquals(3, subgoals.size());
    }

    @Test
    void firstSubgoalIsBottleneckAnalysis() {
        Task task = taskWith(Map.of("goal", "Improve application performance by 3x"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> subgoals = (List<String>) result.getOutputData().get("subgoals");
        assertEquals("Analyze current system performance bottlenecks", subgoals.get(0));
    }

    @Test
    void secondSubgoalIsCachingResearch() {
        Task task = taskWith(Map.of("goal", "Improve application performance by 3x"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> subgoals = (List<String>) result.getOutputData().get("subgoals");
        assertEquals("Research caching and optimization strategies", subgoals.get(1));
    }

    @Test
    void thirdSubgoalIsInfraScaling() {
        Task task = taskWith(Map.of("goal", "Improve application performance by 3x"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> subgoals = (List<String>) result.getOutputData().get("subgoals");
        assertEquals("Evaluate infrastructure scaling options", subgoals.get(2));
    }

    @Test
    void handlesEmptyGoal() {
        Map<String, Object> input = new HashMap<>();
        input.put("goal", "");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<String> subgoals = (List<String>) result.getOutputData().get("subgoals");
        assertNotNull(subgoals);
        assertEquals(3, subgoals.size());
    }

    @Test
    void handlesNullGoal() {
        Map<String, Object> input = new HashMap<>();
        input.put("goal", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("subgoals"));
    }

    @Test
    void handlesMissingGoalInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<String> subgoals = (List<String>) result.getOutputData().get("subgoals");
        assertNotNull(subgoals);
        assertEquals(3, subgoals.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
