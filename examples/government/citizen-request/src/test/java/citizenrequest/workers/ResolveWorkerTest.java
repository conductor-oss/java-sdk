package citizenrequest.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResolveWorkerTest {

    @Test
    void testResolveWorker() {
        ResolveWorker worker = new ResolveWorker();
        assertEquals("ctz_resolve", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("requestId", "REQ-citizen-request-001", "department", "Public Works"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Pothole repaired at Main St", result.getOutputData().get("resolution"));
    }
}
