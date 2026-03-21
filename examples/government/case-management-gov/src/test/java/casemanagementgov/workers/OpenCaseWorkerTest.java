package casemanagementgov.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenCaseWorkerTest {

    @Test
    void testOpenCaseWorker() {
        OpenCaseWorker worker = new OpenCaseWorker();
        assertEquals("cmg_open_case", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("caseType", "regulatory-violation", "reporterId", "INSP-10"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("CASE-case-management-gov-001", result.getOutputData().get("caseId"));
    }
}
