package conductorui.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StepOneWorkerTest {

    private final StepOneWorker worker = new StepOneWorker();

    @Test
    void taskDefName() {
        assertEquals("ui_step_one", worker.getTaskDefName());
    }

    @Test
    void processesUserIdAndAction() {
        Task task = taskWith(Map.of("userId", "user-42", "action", "signup"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed signup for user user-42", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsTimestamp() {
        Task task = taskWith(Map.of("userId", "user-1", "action", "login"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("timestamp"));
        assertTrue(result.getOutputData().get("timestamp").toString().length() > 0);
    }

    @Test
    void outputContainsResultField() {
        Task task = taskWith(Map.of("userId", "user-7", "action", "checkout"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("result"));
        assertTrue(result.getOutputData().get("result").toString().contains("checkout"));
        assertTrue(result.getOutputData().get("result").toString().contains("user-7"));
    }

    @Test
    void defaultsWhenInputMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed default for user unknown", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
