package errornotification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessOrderWorkerTest {

    @Test
    void taskDefName() {
        ProcessOrderWorker worker = new ProcessOrderWorker();
        assertEquals("en_process_order", worker.getTaskDefName());
    }

    @Test
    void succeedsWhenShouldFailIsFalse() {
        ProcessOrderWorker worker = new ProcessOrderWorker();
        Task task = taskWith(Map.of("shouldFail", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order-processed", result.getOutputData().get("result"));
    }

    @Test
    void succeedsWhenShouldFailNotProvided() {
        ProcessOrderWorker worker = new ProcessOrderWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order-processed", result.getOutputData().get("result"));
    }

    @Test
    void failsWhenShouldFailIsTrue() {
        ProcessOrderWorker worker = new ProcessOrderWorker();
        Task task = taskWith(Map.of("shouldFail", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertNotNull(result.getOutputData().get("error"));
        assertNull(result.getOutputData().get("result"));
    }

    @Test
    void failsWhenShouldFailIsStringTrue() {
        ProcessOrderWorker worker = new ProcessOrderWorker();
        Task task = taskWith(Map.of("shouldFail", "true"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertNotNull(result.getOutputData().get("error"));
    }

    @Test
    void succeedsWhenShouldFailIsStringFalse() {
        ProcessOrderWorker worker = new ProcessOrderWorker();
        Task task = taskWith(Map.of("shouldFail", "false"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order-processed", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsResultOnSuccess() {
        ProcessOrderWorker worker = new ProcessOrderWorker();
        Task task = taskWith(Map.of("shouldFail", false));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertFalse(result.getOutputData().containsKey("error"));
    }

    @Test
    void outputContainsErrorOnFailure() {
        ProcessOrderWorker worker = new ProcessOrderWorker();
        Task task = taskWith(Map.of("shouldFail", true));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("error"));
        assertFalse(result.getOutputData().containsKey("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
