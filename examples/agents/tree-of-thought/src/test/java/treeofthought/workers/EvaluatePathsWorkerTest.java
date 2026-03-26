package treeofthought.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EvaluatePathsWorkerTest {

    private final EvaluatePathsWorker worker = new EvaluatePathsWorker();

    @Test
    void taskDefName() {
        assertEquals("tt_evaluate_paths", worker.getTaskDefName());
    }

    @Test
    void selectsEmpiricalAsBestPath() {
        Task task = taskWith(Map.of(
                "pathA", "Solution A",
                "pathB", "Solution B",
                "pathC", "Solution C",
                "scoreA", 0.85,
                "scoreB", 0.72,
                "scoreC", 0.91));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("empirical", result.getOutputData().get("bestPath"));
    }

    @Test
    void returnsBestSolutionFromPathC() {
        Task task = taskWith(Map.of(
                "pathA", "Solution A",
                "pathB", "Solution B",
                "pathC", "The empirical solution",
                "scoreA", 0.85,
                "scoreB", 0.72,
                "scoreC", 0.91));
        TaskResult result = worker.execute(task);

        assertEquals("The empirical solution", result.getOutputData().get("bestSolution"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsAllScores() {
        Task task = taskWith(Map.of(
                "pathA", "A",
                "pathB", "B",
                "pathC", "C",
                "scoreA", 0.85,
                "scoreB", 0.72,
                "scoreC", 0.91));
        TaskResult result = worker.execute(task);

        Map<String, Object> scores = (Map<String, Object>) result.getOutputData().get("scores");
        assertNotNull(scores);
        assertEquals(0.85, scores.get("analytical"));
        assertEquals(0.72, scores.get("creative"));
        assertEquals(0.91, scores.get("empirical"));
    }

    @Test
    void returnsEvaluation() {
        Task task = taskWith(Map.of(
                "pathA", "A",
                "pathB", "B",
                "pathC", "C",
                "scoreA", 0.85,
                "scoreB", 0.72,
                "scoreC", 0.91));
        TaskResult result = worker.execute(task);

        String evaluation = (String) result.getOutputData().get("evaluation");
        assertNotNull(evaluation);
        assertTrue(evaluation.contains("empirical"));
        assertTrue(evaluation.contains("0.91"));
    }

    @Test
    void handlesMissingPaths() {
        Task task = taskWith(Map.of(
                "scoreA", 0.85,
                "scoreB", 0.72,
                "scoreC", 0.91));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("empirical", result.getOutputData().get("bestPath"));
    }

    @Test
    void handlesMissingScores() {
        Task task = taskWith(Map.of(
                "pathA", "A",
                "pathB", "B",
                "pathC", "C"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("empirical", result.getOutputData().get("bestPath"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("empirical", result.getOutputData().get("bestPath"));
        assertNotNull(result.getOutputData().get("scores"));
    }

    @Test
    void handlesNullPathValues() {
        Map<String, Object> input = new HashMap<>();
        input.put("pathA", null);
        input.put("pathB", null);
        input.put("pathC", null);
        input.put("scoreA", 0.85);
        input.put("scoreB", 0.72);
        input.put("scoreC", 0.91);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("empirical", result.getOutputData().get("bestPath"));
    }

    @Test
    void handlesStringScores() {
        Task task = taskWith(Map.of(
                "pathA", "A",
                "pathB", "B",
                "pathC", "C",
                "scoreA", "0.85",
                "scoreB", "0.72",
                "scoreC", "0.91"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("empirical", result.getOutputData().get("bestPath"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
