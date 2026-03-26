package aiorchestrationplatform.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ValidateWorkerTest {

    @Test
    void testValidateWorker() {
        ValidateWorker worker = new ValidateWorker();
        assertEquals("aop_validate", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("result", "test result", "requestType", "summarization"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.95, result.getOutputData().get("quality"));
        assertEquals(true, result.getOutputData().get("safe"));
    }
}
