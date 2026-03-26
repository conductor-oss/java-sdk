package aiorchestrationplatform.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReceiveRequestWorkerTest {

    @Test
    void testReceiveRequestWorker() {
        ReceiveRequestWorker worker = new ReceiveRequestWorker();
        assertEquals("aop_receive_request", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("requestType", "summarization", "priority", "high"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("REQ-ai-orchestration-platform-001", result.getOutputData().get("requestId"));
    }
}
