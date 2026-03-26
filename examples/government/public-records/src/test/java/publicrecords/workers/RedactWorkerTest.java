package publicrecords.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RedactWorkerTest {

    @Test
    void testRedactWorker() {
        RedactWorker worker = new RedactWorker();
        assertEquals("pbr_redact", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("documents", List.of("DOC-A", "DOC-B")));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(12, result.getOutputData().get("redactionsApplied"));
    }
}
