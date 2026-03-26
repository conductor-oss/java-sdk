package finetuneddeployment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateModelWorkerTest {

    private final ValidateModelWorker worker = new ValidateModelWorker();

    @Test
    void taskDefName() {
        assertEquals("ftd_validate_model", worker.getTaskDefName());
    }

    @Test
    void validatesModelAndReturnsChecksum() {
        Task task = taskWith(new HashMap<>(Map.of(
                "modelId", "support-bot-v3",
                "modelVersion", "3.1",
                "baseModel", "llama-2-7b")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
        assertEquals("sha256:a1b2c3d4e5f6", result.getOutputData().get("checksum"));
        assertEquals("7B", result.getOutputData().get("parameterCount"));
        assertEquals("13.5GB", result.getOutputData().get("fileSize"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "modelId", "test-model",
                "modelVersion", "1.0",
                "baseModel", "base")));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("valid"));
        assertNotNull(result.getOutputData().get("checksum"));
        assertNotNull(result.getOutputData().get("parameterCount"));
        assertNotNull(result.getOutputData().get("fileSize"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
