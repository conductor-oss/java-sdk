package aimusicgeneration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ComposeWorkerTest {

    @Test
    void testComposeWorker() {
        ComposeWorker worker = new ComposeWorker();
        assertEquals("amg_compose", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("genre", "jazz", "mood", "upbeat"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(120, result.getOutputData().get("tempo"));
        assertEquals(32, result.getOutputData().get("bars"));
    }
}
