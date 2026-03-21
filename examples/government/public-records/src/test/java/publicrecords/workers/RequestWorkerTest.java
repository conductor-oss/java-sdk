package publicrecords.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestWorkerTest {

    @Test
    void testRequestWorker() {
        RequestWorker worker = new RequestWorker();
        assertEquals("pbr_request", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("requesterId", "MEDIA-01", "recordType", "budget"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("FOIA-public-records-001", result.getOutputData().get("requestId"));
    }
}
