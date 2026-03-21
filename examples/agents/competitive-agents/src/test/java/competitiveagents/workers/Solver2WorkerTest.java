package competitiveagents.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Solver2WorkerTest {

    private final Solver2Worker worker = new Solver2Worker();

    @Test
    void taskDefName() {
        assertEquals("comp_solver_2", worker.getTaskDefName());
    }

    @Test
    void returnsAnalyticalSolution() {
        Task task = taskWith(Map.of("problem", "Reduce costs", "criteria", "cost,innovation"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> solution = (Map<String, Object>) result.getOutputData().get("solution");
        assertNotNull(solution);
        assertEquals("analytical", solution.get("approach"));
        assertEquals("Data-Driven Optimization Framework", solution.get("title"));
        assertEquals("$85K", solution.get("estimatedCost"));
        assertEquals("4 months", solution.get("timeline"));
        assertEquals(7, solution.get("innovationScore"));
        assertEquals("low", solution.get("riskLevel"));
    }

    @Test
    void solutionContainsDescription() {
        Task task = taskWith(Map.of("problem", "Improve efficiency"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> solution = (Map<String, Object>) result.getOutputData().get("solution");
        assertNotNull(solution.get("description"));
        assertTrue(((String) solution.get("description")).length() > 10);
    }

    @Test
    void solutionContainsAllExpectedFields() {
        Task task = taskWith(Map.of("problem", "Test problem"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> solution = (Map<String, Object>) result.getOutputData().get("solution");
        assertEquals(7, solution.size());
        assertTrue(solution.containsKey("approach"));
        assertTrue(solution.containsKey("title"));
        assertTrue(solution.containsKey("description"));
        assertTrue(solution.containsKey("estimatedCost"));
        assertTrue(solution.containsKey("timeline"));
        assertTrue(solution.containsKey("innovationScore"));
        assertTrue(solution.containsKey("riskLevel"));
    }

    @Test
    void handlesNullProblem() {
        Map<String, Object> input = new HashMap<>();
        input.put("problem", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("solution"));
    }

    @Test
    void handlesMissingProblem() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("solution"));
    }

    @Test
    void handlesBlankProblem() {
        Task task = taskWith(Map.of("problem", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("solution"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
