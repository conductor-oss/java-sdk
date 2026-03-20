package aiguardrails.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class InputCheckWorkerTest {

    @Test
    void testInputCheckWorker() {
        InputCheckWorker worker = new InputCheckWorker();
        assertEquals("grl_input_check", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("userPrompt", "test", "userId", "USR-TEST"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("safe"));
        assertEquals(false, result.getOutputData().get("piiDetected"));
    }
}
