package treeofthought.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PathAWorkerTest {

    private final PathAWorker worker = new PathAWorker();

    @Test
    void taskDefName() {
        assertEquals("tt_path_a", worker.getTaskDefName());
    }

    @Test
    void returnsAnalyticalSolution() {
        Task task = taskWith(Map.of("problem", "Handle 10x traffic", "approach", "analytical"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Use load balancer with auto-scaling groups, deploy across 3 AZs, implement health checks with 30s intervals",
                result.getOutputData().get("solution"));
    }

    @Test
    void returnsApproachLabel() {
        Task task = taskWith(Map.of("problem", "Handle 10x traffic", "approach", "analytical"));
        TaskResult result = worker.execute(task);

        assertEquals("analytical", result.getOutputData().get("approach"));
    }

    @Test
    void returnsConfidenceScore() {
        Task task = taskWith(Map.of("problem", "Handle 10x traffic", "approach", "analytical"));
        TaskResult result = worker.execute(task);

        assertEquals(0.85, result.getOutputData().get("confidence"));
    }

    @Test
    void handlesMissingProblem() {
        Task task = taskWith(Map.of("approach", "analytical"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("solution"));
    }

    @Test
    void handlesMissingApproach() {
        Task task = taskWith(Map.of("problem", "Handle 10x traffic"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("analytical", result.getOutputData().get("approach"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("analytical", result.getOutputData().get("approach"));
        assertEquals(0.85, result.getOutputData().get("confidence"));
    }

    @Test
    void handlesNullProblem() {
        Map<String, Object> input = new HashMap<>();
        input.put("problem", null);
        input.put("approach", "analytical");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("solution"));
    }

    @Test
    void solutionMentionsLoadBalancer() {
        Task task = taskWith(Map.of("problem", "Scale system", "approach", "analytical"));
        TaskResult result = worker.execute(task);

        String solution = (String) result.getOutputData().get("solution");
        assertTrue(solution.contains("load balancer"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
