package treeofthought.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PathBWorkerTest {

    private final PathBWorker worker = new PathBWorker();

    @Test
    void taskDefName() {
        assertEquals("tt_path_b", worker.getTaskDefName());
    }

    @Test
    void returnsCreativeSolution() {
        Task task = taskWith(Map.of("problem", "Handle 10x traffic", "approach", "creative"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Edge computing with CDN-based logic, serverless functions at edge nodes, predictive pre-warming",
                result.getOutputData().get("solution"));
    }

    @Test
    void returnsApproachLabel() {
        Task task = taskWith(Map.of("problem", "Handle 10x traffic", "approach", "creative"));
        TaskResult result = worker.execute(task);

        assertEquals("creative", result.getOutputData().get("approach"));
    }

    @Test
    void returnsConfidenceScore() {
        Task task = taskWith(Map.of("problem", "Handle 10x traffic", "approach", "creative"));
        TaskResult result = worker.execute(task);

        assertEquals(0.72, result.getOutputData().get("confidence"));
    }

    @Test
    void handlesMissingProblem() {
        Task task = taskWith(Map.of("approach", "creative"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("solution"));
    }

    @Test
    void handlesMissingApproach() {
        Task task = taskWith(Map.of("problem", "Handle 10x traffic"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("creative", result.getOutputData().get("approach"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("creative", result.getOutputData().get("approach"));
        assertEquals(0.72, result.getOutputData().get("confidence"));
    }

    @Test
    void handlesNullProblem() {
        Map<String, Object> input = new HashMap<>();
        input.put("problem", null);
        input.put("approach", "creative");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("solution"));
    }

    @Test
    void solutionMentionsEdgeComputing() {
        Task task = taskWith(Map.of("problem", "Scale system", "approach", "creative"));
        TaskResult result = worker.execute(task);

        String solution = (String) result.getOutputData().get("solution");
        assertTrue(solution.contains("Edge computing"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
