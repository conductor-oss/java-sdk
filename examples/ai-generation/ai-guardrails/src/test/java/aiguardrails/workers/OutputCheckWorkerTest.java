package aiguardrails.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class OutputCheckWorkerTest {

    @Test
    void testOutputCheckWorker() {
        OutputCheckWorker worker = new OutputCheckWorker();
        assertEquals("grl_output_check", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("response", "test response"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("safe"));
        assertEquals(0.01, result.getOutputData().get("toxicity"));
    }
}
