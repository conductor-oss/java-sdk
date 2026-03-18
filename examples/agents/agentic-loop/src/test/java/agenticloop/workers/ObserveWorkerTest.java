package agenticloop.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ObserveWorkerTest {

    private final ObserveWorker worker = new ObserveWorker();

    @Test
    void taskDefName() {
        assertEquals("al_observe", worker.getTaskDefName());
    }

    @Test
    void iteration1ReturnsObservation1() {
        Task task = taskWith(Map.of("actionResult", "Some action result", "iteration", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Information gathering phase complete; sufficient data collected to proceed with analysis",
                result.getOutputData().get("observation"));
        assertEquals("advancing", result.getOutputData().get("goalProgress"));
    }

    @Test
    void iteration2ReturnsObservation2() {
        Task task = taskWith(Map.of("actionResult", "Analysis done", "iteration", 2));
        TaskResult result = worker.execute(task);

        assertEquals("Pattern analysis reveals clear structure; ready to formulate recommendations",
                result.getOutputData().get("observation"));
        assertEquals("advancing", result.getOutputData().get("goalProgress"));
    }

    @Test
    void iteration3ReturnsObservation3() {
        Task task = taskWith(Map.of("actionResult", "Synthesis complete", "iteration", 3));
        TaskResult result = worker.execute(task);

        assertEquals("Synthesis complete; goal has been fully addressed with comprehensive recommendations",
                result.getOutputData().get("observation"));
        assertEquals("advancing", result.getOutputData().get("goalProgress"));
    }

    @Test
    void outputContainsObservationAndProgress() {
        Task task = taskWith(Map.of("actionResult", "Result", "iteration", 1));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("observation"));
        assertTrue(result.getOutputData().containsKey("goalProgress"));
    }

    @Test
    void handlesNullActionResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("actionResult", null);
        input.put("iteration", 1);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("observation"));
        assertEquals("advancing", result.getOutputData().get("goalProgress"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("observation"));
        assertEquals("advancing", result.getOutputData().get("goalProgress"));
    }

    @Test
    void handlesBlankActionResult() {
        Task task = taskWith(Map.of("actionResult", "   ", "iteration", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("observation"));
    }

    @Test
    void handlesStringIteration() {
        Task task = taskWith(Map.of("actionResult", "Result", "iteration", "3"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Synthesis complete; goal has been fully addressed with comprehensive recommendations",
                result.getOutputData().get("observation"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
