package treeofthought.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PathCWorkerTest {

    private final PathCWorker worker = new PathCWorker();

    @Test
    void taskDefName() {
        assertEquals("tt_path_c", worker.getTaskDefName());
    }

    @Test
    void returnsEmpiricalSolution() {
        Task task = taskWith(Map.of("problem", "Handle 10x traffic", "approach", "empirical"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Based on traffic analysis: 80% requests from 3 regions, deploy regional clusters with geo-routing, cache top 1000 queries",
                result.getOutputData().get("solution"));
    }

    @Test
    void returnsApproachLabel() {
        Task task = taskWith(Map.of("problem", "Handle 10x traffic", "approach", "empirical"));
        TaskResult result = worker.execute(task);

        assertEquals("empirical", result.getOutputData().get("approach"));
    }

    @Test
    void returnsConfidenceScore() {
        Task task = taskWith(Map.of("problem", "Handle 10x traffic", "approach", "empirical"));
        TaskResult result = worker.execute(task);

        assertEquals(0.91, result.getOutputData().get("confidence"));
    }

    @Test
    void handlesMissingProblem() {
        Task task = taskWith(Map.of("approach", "empirical"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("solution"));
    }

    @Test
    void handlesMissingApproach() {
        Task task = taskWith(Map.of("problem", "Handle 10x traffic"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("empirical", result.getOutputData().get("approach"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("empirical", result.getOutputData().get("approach"));
        assertEquals(0.91, result.getOutputData().get("confidence"));
    }

    @Test
    void handlesNullProblem() {
        Map<String, Object> input = new HashMap<>();
        input.put("problem", null);
        input.put("approach", "empirical");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("solution"));
    }

    @Test
    void solutionMentionsTrafficAnalysis() {
        Task task = taskWith(Map.of("problem", "Scale system", "approach", "empirical"));
        TaskResult result = worker.execute(task);

        String solution = (String) result.getOutputData().get("solution");
        assertTrue(solution.contains("traffic analysis"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
