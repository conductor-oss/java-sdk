package citizenrequest.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClassifyWorkerTest {

    @Test
    void testClassifyWorker() {
        ClassifyWorker worker = new ClassifyWorker();
        assertEquals("ctz_classify", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("requestId", "REQ-citizen-request-001", "description", "Large pothole"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("infrastructure", result.getOutputData().get("category"));
        assertEquals("medium", result.getOutputData().get("priority"));
    }
}
