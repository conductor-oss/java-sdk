package citizenrequest.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SubmitWorkerTest {

    @Test
    void testSubmitWorker() {
        SubmitWorker worker = new SubmitWorker();
        assertEquals("ctz_submit", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("citizenId", "CIT-200", "description", "Large pothole"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("REQ-citizen-request-001", result.getOutputData().get("requestId"));
    }
}
