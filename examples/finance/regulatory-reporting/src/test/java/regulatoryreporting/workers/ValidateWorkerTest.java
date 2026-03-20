package regulatoryreporting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ValidateWorkerTest {
    private final ValidateWorker worker = new ValidateWorker();
    @Test void taskDefName() { assertEquals("reg_validate", worker.getTaskDefName()); }
    @Test void validatesSuccessfully() {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("data", Map.of("totalAssets", 1000, "capitalRatio", 12.5), "reportType", "CALL")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("errorCount"));
        assertEquals(true, result.getOutputData().get("passed"));
    }
    @Test void detectsMissingFields() {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>(); input.put("data", Map.of()); input.put("reportType", "CALL");
        task.setInputData(input);
        TaskResult result = worker.execute(task);
        assertEquals(2, result.getOutputData().get("errorCount"));
    }
}
