package switchdefaultcase.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UnknownMethodWorkerTest {

    private final UnknownMethodWorker worker = new UnknownMethodWorker();

    @Test
    void taskDefName() {
        assertEquals("dc_unknown_method", worker.getTaskDefName());
    }

    @Test
    void returnsManualReviewHandler() {
        Task task = taskWith(Map.of("method", "paypal"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("manual_review", result.getOutputData().get("handler"));
        assertEquals(true, result.getOutputData().get("needsHuman"));
    }

    @Test
    void outputIsAlwaysDeterministic() {
        Task task1 = taskWith(Map.of("method", "paypal"));
        Task task2 = taskWith(Map.of("method", "venmo"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("handler"), result2.getOutputData().get("handler"));
        assertEquals(result1.getOutputData().get("needsHuman"), result2.getOutputData().get("needsHuman"));
    }

    @Test
    void statusIsAlwaysCompleted() {
        Task task = taskWith(Map.of("method", "wire"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
