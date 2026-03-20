package dataexportrequest.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PackageDataWorkerTest {
    private final PackageDataWorker worker = new PackageDataWorker();

    @Test void taskDefName() { assertEquals("der_package", worker.getTaskDefName()); }

    @Test void packagesData() {
        Task task = taskWith(Map.of("data", Map.of(), "format", "json"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().get("packageUrl").toString().endsWith(".json"));
    }

    @Test void includesSize() {
        Task task = taskWith(Map.of("data", Map.of(), "format", "csv"));
        TaskResult result = worker.execute(task);
        assertEquals(2458624, result.getOutputData().get("sizeBytes"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
