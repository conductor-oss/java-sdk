package dataexportrequest.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ValidateExportWorkerTest {
    private final ValidateExportWorker worker = new ValidateExportWorker();

    @Test void taskDefName() { assertEquals("der_validate", worker.getTaskDefName()); }

    @Test void validatesRequest() {
        Task task = taskWith(Map.of("userId", "USR-123", "exportFormat", "json"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
    }

    @Test void identityVerified() {
        Task task = taskWith(Map.of("userId", "USR-123", "exportFormat", "csv"));
        TaskResult result = worker.execute(task);
        assertEquals("verified", result.getOutputData().get("identity"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
