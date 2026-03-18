package exactlyonce.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExoCommitWorkerTest {

    private final ExoCommitWorker worker = new ExoCommitWorker();

    @BeforeEach
    void setUp() {
        ExoCheckStateWorker.clearState();
    }

    @Test
    void taskDefName() {
        assertEquals("exo_commit", worker.getTaskDefName());
    }

    @Test
    void commitsSuccessfully() {
        Task task = taskWith(Map.of("messageId", "msg-1", "lockToken", "tok-1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("committed"));
    }

    @Test
    void updatesStateStoreToCompleted() {
        Task task = taskWith(Map.of("messageId", "msg-2", "lockToken", "tok-2"));
        worker.execute(task);

        assertEquals("completed", ExoCheckStateWorker.STATE_STORE.get("msg-2"));
    }

    @Test
    void stateTransitionShowsPendingToCompleted() {
        Task task = taskWith(Map.of("messageId", "msg-3"));
        TaskResult result = worker.execute(task);

        String transition = (String) result.getOutputData().get("stateTransition");
        assertEquals("pending -> completed", transition);
    }

    @Test
    void commitTimestampIsRealIso8601() {
        Task task = taskWith(Map.of("messageId", "msg-4"));
        TaskResult result = worker.execute(task);

        String ts = (String) result.getOutputData().get("commitTimestamp");
        assertNotNull(ts);
        assertTrue(ts.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z"),
                "Timestamp should be ISO-8601, got: " + ts);
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("committed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
