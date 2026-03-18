package agenticloop.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThinkWorkerTest {

    private final ThinkWorker worker = new ThinkWorker();

    @Test
    void taskDefName() {
        assertEquals("al_think", worker.getTaskDefName());
    }

    @Test
    void iteration1ReturnsPlan1() {
        Task task = taskWith(Map.of("goal", "Research distributed systems", "iteration", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Research and gather information on the topic", result.getOutputData().get("plan"));
        assertEquals(1, result.getOutputData().get("iteration"));
    }

    @Test
    void iteration2ReturnsPlan2() {
        Task task = taskWith(Map.of("goal", "Research distributed systems", "iteration", 2));
        TaskResult result = worker.execute(task);

        assertEquals("Analyze gathered data and identify key patterns", result.getOutputData().get("plan"));
        assertEquals(2, result.getOutputData().get("iteration"));
    }

    @Test
    void iteration3ReturnsPlan3() {
        Task task = taskWith(Map.of("goal", "Research distributed systems", "iteration", 3));
        TaskResult result = worker.execute(task);

        assertEquals("Synthesize findings into actionable recommendations", result.getOutputData().get("plan"));
        assertEquals(3, result.getOutputData().get("iteration"));
    }

    @Test
    void outputContainsPlanAndIteration() {
        Task task = taskWith(Map.of("goal", "Test goal", "iteration", 1));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("plan"));
        assertTrue(result.getOutputData().containsKey("iteration"));
    }

    @Test
    void handlesNullGoal() {
        Map<String, Object> input = new HashMap<>();
        input.put("goal", null);
        input.put("iteration", 1);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("plan"));
    }

    @Test
    void handlesMissingIteration() {
        Task task = taskWith(Map.of("goal", "Test goal"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("iteration"));
        assertEquals("Research and gather information on the topic", result.getOutputData().get("plan"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("plan"));
        assertEquals(1, result.getOutputData().get("iteration"));
    }

    @Test
    void handlesStringIteration() {
        Task task = taskWith(Map.of("goal", "Test goal", "iteration", "2"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("iteration"));
        assertEquals("Analyze gathered data and identify key patterns", result.getOutputData().get("plan"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
