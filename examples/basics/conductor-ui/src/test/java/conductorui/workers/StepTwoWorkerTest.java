package conductorui.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StepTwoWorkerTest {

    private final StepTwoWorker worker = new StepTwoWorker();

    @Test
    void taskDefName() {
        assertEquals("ui_step_two", worker.getTaskDefName());
    }

    @Test
    void enrichesPreviousResult() {
        Task task = taskWith(Map.of("previousResult", "Processed signup for user user-42"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Enriched: Processed signup for user user-42",
                result.getOutputData().get("result"));
    }

    @Test
    void outputContainsEnrichmentMetadata() {
        Task task = taskWith(Map.of("previousResult", "some data"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("enriched"));
    }

    @Test
    void scoreIsBasedOnWordCount() {
        // "data" is 1 word -> score = min(1.0, 1 * 0.1) = 0.1
        Task task = taskWith(Map.of("previousResult", "data"));
        TaskResult result = worker.execute(task);

        assertEquals(0.1, (double) result.getOutputData().get("score"), 0.001);
    }

    @Test
    void scoreCapsAtOne() {
        // 15 words -> score = min(1.0, 15 * 0.1) = 1.0
        Task task = taskWith(Map.of("previousResult",
                "one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen"));
        TaskResult result = worker.execute(task);

        assertEquals(1.0, (double) result.getOutputData().get("score"), 0.001);
    }

    @Test
    void scoreIsConsistentAcrossExecutions() {
        Task task1 = taskWith(Map.of("previousResult", "same input text"));
        Task task2 = taskWith(Map.of("previousResult", "same input text"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("score"),
                result2.getOutputData().get("score"));
    }

    @Test
    void scoreVariesWithDifferentInputLengths() {
        Task task1 = taskWith(Map.of("previousResult", "short"));
        Task task2 = taskWith(Map.of("previousResult", "a much longer input with many words"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        double score1 = (double) result1.getOutputData().get("score");
        double score2 = (double) result2.getOutputData().get("score");
        assertTrue(score2 > score1, "Longer input should produce higher score");
    }

    @Test
    void defaultsWhenPreviousResultMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Enriched: no previous data", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
