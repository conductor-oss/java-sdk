package agenticloop.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SummarizeWorkerTest {

    private final SummarizeWorker worker = new SummarizeWorker();

    @Test
    void taskDefName() {
        assertEquals("al_summarize", worker.getTaskDefName());
    }

    @Test
    void summarizesWithGoalAndIterations() {
        Task task = taskWith(Map.of(
                "goal", "Research best practices for distributed systems",
                "totalIterations", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Achieved goal 'Research best practices for distributed systems' through 3 think-act-observe cycles",
                result.getOutputData().get("summary"));
    }

    @Test
    void outputContainsSummary() {
        Task task = taskWith(Map.of("goal", "Test goal", "totalIterations", 3));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("summary"));
        assertEquals(1, result.getOutputData().size());
    }

    @Test
    void summaryIncludesGoalText() {
        Task task = taskWith(Map.of("goal", "Build a microservice", "totalIterations", 5));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("Build a microservice"));
        assertTrue(summary.contains("5 think-act-observe cycles"));
    }

    @Test
    void handlesNullGoal() {
        Map<String, Object> input = new HashMap<>();
        input.put("goal", null);
        input.put("totalIterations", 3);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("No goal specified"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("No goal specified"));
        assertTrue(summary.contains("3 think-act-observe cycles"));
    }

    @Test
    void handlesBlankGoal() {
        Task task = taskWith(Map.of("goal", "  ", "totalIterations", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("No goal specified"));
    }

    @Test
    void handlesStringIterations() {
        Task task = taskWith(Map.of("goal", "Test goal", "totalIterations", "5"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("5 think-act-observe cycles"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
