package serverlessorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SvlInvokeScoreWorkerTest {

    private final SvlInvokeScoreWorker worker = new SvlInvokeScoreWorker();

    @Test
    void taskDefName() {
        assertEquals("svl_invoke_score", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of(
                "functionArn", "arn:aws:lambda:us-east-1:123:function:compute-score",
                "enrichedData", Map.of("userTier", "premium")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsScore() {
        Task task = taskWith(Map.of("enrichedData", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(87.5, result.getOutputData().get("score"));
    }

    @Test
    void outputContainsConfidence() {
        Task task = taskWith(Map.of("enrichedData", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(0.92, result.getOutputData().get("confidence"));
    }

    @Test
    void outputContainsBilledMs() {
        Task task = taskWith(Map.of("enrichedData", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(80, result.getOutputData().get("billedMs"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void scoreIsDouble() {
        Task task = taskWith(Map.of("enrichedData", Map.of()));
        TaskResult result = worker.execute(task);
        assertInstanceOf(Double.class, result.getOutputData().get("score"));
    }

    @Test
    void deterministicOutput() {
        Task t1 = taskWith(Map.of("enrichedData", Map.of()));
        Task t2 = taskWith(Map.of("enrichedData", Map.of()));
        TaskResult r1 = worker.execute(t1);
        TaskResult r2 = worker.execute(t2);
        assertEquals(r1.getOutputData().get("score"), r2.getOutputData().get("score"));
        assertEquals(r1.getOutputData().get("confidence"), r2.getOutputData().get("confidence"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
