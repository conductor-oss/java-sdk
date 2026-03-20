package snssqsintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessMessageWorkerTest {

    private final ProcessMessageWorker worker = new ProcessMessageWorker();

    @Test
    void taskDefName() {
        assertEquals("sns_process_message", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("messageBody", "test-message", "receiptHandle", "rh-123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals(true, result.getOutputData().get("deletedFromQueue"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
