package escalationtimer.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SubmitWorkerTest {

    @Test
    void taskDefName() {
        SubmitWorker worker = new SubmitWorker();
        assertEquals("et_submit", worker.getTaskDefName());
    }

    @Test
    void submitsRequestSuccessfully() {
        SubmitWorker worker = new SubmitWorker();
        Task task = taskWith(Map.of("requestId", "REQ-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("submitted"));
    }

    @Test
    void handlesEmptyInput() {
        SubmitWorker worker = new SubmitWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("submitted"));
    }

    @Test
    void handlesNonStringRequestId() {
        SubmitWorker worker = new SubmitWorker();
        Task task = taskWith(Map.of("requestId", 42));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("submitted"));
    }

    @Test
    void outputIsDeterministic() {
        SubmitWorker worker = new SubmitWorker();

        Task task1 = taskWith(Map.of("requestId", "REQ-DET"));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of("requestId", "REQ-DET"));
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getStatus(), result2.getStatus());
        assertEquals(result1.getOutputData().get("submitted"), result2.getOutputData().get("submitted"));
    }

    @Test
    void alwaysReturnsSubmittedTrue() {
        SubmitWorker worker = new SubmitWorker();
        Task task = taskWith(Map.of("requestId", "REQ-ANY"));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("submitted"));
    }

    @Test
    void handlesMultipleInvocations() {
        SubmitWorker worker = new SubmitWorker();

        for (int i = 0; i < 3; i++) {
            Task task = taskWith(Map.of("requestId", "REQ-" + i));
            TaskResult result = worker.execute(task);
            assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
            assertEquals(true, result.getOutputData().get("submitted"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
