package aidatalabeling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReconcileWorkerTest {

    @Test
    void testReconcile() {
        ReconcileWorker worker = new ReconcileWorker();
        assertEquals("adl_reconcile", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("labels1", "l1", "labels2", "l2"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(500, result.getOutputData().get("totalLabeled"));
        assertEquals(0.94, result.getOutputData().get("agreement"));
    }
}
