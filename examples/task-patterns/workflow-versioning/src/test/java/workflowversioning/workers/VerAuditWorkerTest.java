package workflowversioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerAuditWorkerTest {

    private final VerAuditWorker worker = new VerAuditWorker();

    @Test
    void taskDefName() {
        assertEquals("ver_audit", worker.getTaskDefName());
    }

    @Test
    void auditsResult() {
        Task task = taskWith(Map.of("finalResult", 110));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(110, result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("audited"));
    }

    @Test
    void auditsZeroResult() {
        Task task = taskWith(Map.of("finalResult", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("audited"));
    }

    @Test
    void defaultsToZeroWhenFinalResultMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("audited"));
    }

    @Test
    void auditsNegativeResult() {
        Task task = taskWith(Map.of("finalResult", -50));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(-50, result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("audited"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
