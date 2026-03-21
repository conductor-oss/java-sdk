package ragqualitygates.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RejectWorkerTest {

    private final RejectWorker worker = new RejectWorker();

    @Test
    void taskDefName() {
        assertEquals("qg_reject", worker.getTaskDefName());
    }

    @Test
    void rejectsWithRelevanceReason() {
        Task task = taskWith(new HashMap<>(Map.of(
                "reason", "relevance_below_threshold",
                "score", 0.45
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("rejected"));
        assertEquals("relevance_below_threshold", result.getOutputData().get("reason"));
        assertEquals(0.45, result.getOutputData().get("score"));
    }

    @Test
    void rejectsWithFaithfulnessReason() {
        Task task = taskWith(new HashMap<>(Map.of(
                "reason", "faithfulness_below_threshold",
                "score", 0.33
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("rejected"));
        assertEquals("faithfulness_below_threshold", result.getOutputData().get("reason"));
        assertEquals(0.33, result.getOutputData().get("score"));
    }

    @Test
    void handlesNullReason() {
        Task task = taskWith(new HashMap<>(Map.of("score", 0.5)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("rejected"));
        assertEquals("unknown", result.getOutputData().get("reason"));
    }

    @Test
    void handlesNullScore() {
        Task task = taskWith(new HashMap<>(Map.of("reason", "test_reason")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("rejected"));
        assertEquals(0.0, result.getOutputData().get("score"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("rejected"));
        assertEquals("unknown", result.getOutputData().get("reason"));
        assertEquals(0.0, result.getOutputData().get("score"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
