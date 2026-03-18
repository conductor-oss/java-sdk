package exactlyonce.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExoCheckStateWorkerTest {

    private final ExoCheckStateWorker worker = new ExoCheckStateWorker();

    @BeforeEach
    void setUp() {
        ExoCheckStateWorker.clearState();
    }

    @Test
    void taskDefName() {
        assertEquals("exo_check_state", worker.getTaskDefName());
    }

    @Test
    void newMessageReturnsPending() {
        Task task = taskWith(Map.of("messageId", "msg-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("pending", result.getOutputData().get("currentState"));
        assertEquals(false, result.getOutputData().get("alreadyProcessed"));
    }

    @Test
    void completedMessageReturnsAlreadyProcessed() {
        ExoCheckStateWorker.STATE_STORE.put("msg-002", "completed");

        Task task = taskWith(Map.of("messageId", "msg-002"));
        TaskResult result = worker.execute(task);

        assertEquals("completed", result.getOutputData().get("currentState"));
        assertEquals(true, result.getOutputData().get("alreadyProcessed"));
    }

    @Test
    void sequenceNumberIncrements() {
        Task task1 = taskWith(Map.of("messageId", "msg-003"));
        TaskResult r1 = worker.execute(task1);
        int seq1 = (int) r1.getOutputData().get("sequenceNumber");

        Task task2 = taskWith(Map.of("messageId", "msg-003"));
        TaskResult r2 = worker.execute(task2);
        int seq2 = (int) r2.getOutputData().get("sequenceNumber");

        assertEquals(seq1 + 1, seq2, "Sequence number should increment on each check");
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("currentState"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
