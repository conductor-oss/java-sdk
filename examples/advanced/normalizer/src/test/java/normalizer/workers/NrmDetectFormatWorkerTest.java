package normalizer.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NrmDetectFormatWorkerTest {

    private final NrmDetectFormatWorker worker = new NrmDetectFormatWorker();

    @Test
    void taskDefName() {
        assertEquals("nrm_detect_format", worker.getTaskDefName());
    }

    @Test
    void executesSuccessfully() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("test", "value")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsExpectedKey() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("test", "value")));
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("detectedFormat"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }
}