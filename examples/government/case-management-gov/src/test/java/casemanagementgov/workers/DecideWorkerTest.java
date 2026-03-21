package casemanagementgov.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DecideWorkerTest {

    @Test
    void testDecideWorker() {
        DecideWorker worker = new DecideWorker();
        assertEquals("cmg_decide", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("caseId", "CASE-case-management-gov-001", "evaluation", "substantiated"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("corrective-action", result.getOutputData().get("decision"));
    }
}
