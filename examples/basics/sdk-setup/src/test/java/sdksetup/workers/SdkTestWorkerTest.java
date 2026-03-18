package sdksetup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SdkTestWorkerTest {

    private final SdkTestWorker worker = new SdkTestWorker();

    @Test
    void taskDefName() {
        assertEquals("sdk_test_task", worker.getTaskDefName());
    }

    @Test
    void returnsResultForCheckInput() {
        Task task = taskWith(Map.of("check", "connectivity"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(
                "SDK check 'connectivity' passed — conductor-client 5.0.1 is working",
                result.getOutputData().get("result")
        );
    }

    @Test
    void defaultsWhenCheckMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(
                "SDK check 'default' passed — conductor-client 5.0.1 is working",
                result.getOutputData().get("result")
        );
    }

    @Test
    void defaultsWhenCheckBlank() {
        Task task = taskWith(Map.of("check", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().get("result").toString().contains("'default'"));
    }

    @Test
    void outputContainsResultField() {
        Task task = taskWith(Map.of("check", "smoke"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("result"));
        assertTrue(result.getOutputData().get("result").toString().contains("smoke"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
