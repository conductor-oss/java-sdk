package exactlyonce.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExoProcessWorkerTest {

    private final ExoProcessWorker worker = new ExoProcessWorker();

    @Test
    void taskDefName() {
        assertEquals("exo_process", worker.getTaskDefName());
    }

    @Test
    void processesNewMessage() {
        Task task = taskWith(Map.of("messageId", "msg-1", "payload", "data", "currentState", "pending"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void skipsAlreadyCompletedMessage() {
        Task task = taskWith(Map.of("messageId", "msg-2", "currentState", "completed"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("processed"));
        assertEquals(true, result.getOutputData().get("skipped"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void deterministicOutputForSameMessageId() {
        Task task1 = taskWith(Map.of("messageId", "msg-3", "currentState", "pending"));
        Task task2 = taskWith(Map.of("messageId", "msg-3", "currentState", "pending"));

        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        Map<String, Object> result1 = (Map<String, Object>) r1.getOutputData().get("result");
        Map<String, Object> result2 = (Map<String, Object>) r2.getOutputData().get("result");

        assertEquals(result1.get("hash"), result2.get("hash"),
                "Same messageId should produce same hash");
    }

    @SuppressWarnings("unchecked")
    @Test
    void differentMessagesProduceDifferentHashes() {
        Task taskA = taskWith(Map.of("messageId", "msg-A", "currentState", "pending"));
        Task taskB = taskWith(Map.of("messageId", "msg-B", "currentState", "pending"));

        Map<String, Object> rA = (Map<String, Object>) worker.execute(taskA).getOutputData().get("result");
        Map<String, Object> rB = (Map<String, Object>) worker.execute(taskB).getOutputData().get("result");

        assertNotEquals(rA.get("hash"), rB.get("hash"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
