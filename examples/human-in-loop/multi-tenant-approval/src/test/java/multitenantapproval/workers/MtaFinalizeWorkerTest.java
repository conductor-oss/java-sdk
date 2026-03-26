package multitenantapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MtaFinalizeWorkerTest {

    @Test
    void taskDefName() {
        MtaFinalizeWorker worker = new MtaFinalizeWorker();
        assertEquals("mta_finalize", worker.getTaskDefName());
    }

    @Test
    void returnsProcessedTrue() {
        MtaFinalizeWorker worker = new MtaFinalizeWorker();
        Task task = taskWith(Map.of("tenantId", "startup-co", "approvalLevel", "manager"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsProcessedKey() {
        MtaFinalizeWorker worker = new MtaFinalizeWorker();
        Task task = taskWith(Map.of("tenantId", "enterprise-corp", "approvalLevel", "executive"));

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("processed"));
    }

    @Test
    void alwaysCompletes() {
        MtaFinalizeWorker worker = new MtaFinalizeWorker();
        Task task = taskWith(Map.of("tenantId", "small-biz", "approvalLevel", "none"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void completesWithoutOptionalInputs() {
        MtaFinalizeWorker worker = new MtaFinalizeWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void deterministic() {
        MtaFinalizeWorker worker = new MtaFinalizeWorker();

        Task task1 = taskWith(Map.of("tenantId", "startup-co", "approvalLevel", "manager"));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of("tenantId", "startup-co", "approvalLevel", "manager"));
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("processed"),
                result2.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
