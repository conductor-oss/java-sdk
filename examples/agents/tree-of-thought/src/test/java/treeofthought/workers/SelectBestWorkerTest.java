package treeofthought.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SelectBestWorkerTest {

    private final SelectBestWorker worker = new SelectBestWorker();

    @Test
    void taskDefName() {
        assertEquals("tt_select_best", worker.getTaskDefName());
    }

    @Test
    void returnsSelectedPath() {
        Task task = taskWith(Map.of(
                "bestPath", "empirical",
                "bestSolution", "Deploy regional clusters",
                "evaluation", "Path empirical scored highest at 0.91"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("empirical", result.getOutputData().get("selectedPath"));
    }

    @Test
    void returnsSolution() {
        Task task = taskWith(Map.of(
                "bestPath", "empirical",
                "bestSolution", "Deploy regional clusters with geo-routing",
                "evaluation", "Path empirical scored highest at 0.91"));
        TaskResult result = worker.execute(task);

        assertEquals("Deploy regional clusters with geo-routing", result.getOutputData().get("solution"));
    }

    @Test
    void handlesMissingBestPath() {
        Task task = taskWith(Map.of(
                "bestSolution", "Some solution",
                "evaluation", "Some evaluation"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("selectedPath"));
    }

    @Test
    void handlesMissingSolution() {
        Task task = taskWith(Map.of(
                "bestPath", "empirical",
                "evaluation", "Some evaluation"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("No solution available", result.getOutputData().get("solution"));
    }

    @Test
    void handlesMissingEvaluation() {
        Task task = taskWith(Map.of(
                "bestPath", "empirical",
                "bestSolution", "Deploy clusters"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("empirical", result.getOutputData().get("selectedPath"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("selectedPath"));
        assertEquals("No solution available", result.getOutputData().get("solution"));
    }

    @Test
    void handlesNullValues() {
        Map<String, Object> input = new HashMap<>();
        input.put("bestPath", null);
        input.put("bestSolution", null);
        input.put("evaluation", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("selectedPath"));
        assertEquals("No solution available", result.getOutputData().get("solution"));
    }

    @Test
    void preservesExactSolutionText() {
        String solution = "Based on traffic analysis: 80% requests from 3 regions, deploy regional clusters with geo-routing, cache top 1000 queries";
        Task task = taskWith(Map.of(
                "bestPath", "empirical",
                "bestSolution", solution,
                "evaluation", "Path empirical scored highest"));
        TaskResult result = worker.execute(task);

        assertEquals(solution, result.getOutputData().get("solution"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
