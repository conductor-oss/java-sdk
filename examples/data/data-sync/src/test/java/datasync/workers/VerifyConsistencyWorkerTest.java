package datasync.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyConsistencyWorkerTest {

    private final VerifyConsistencyWorker worker = new VerifyConsistencyWorker();

    @Test
    void taskDefName() {
        assertEquals("sy_verify_consistency", worker.getTaskDefName());
    }

    @Test
    void consistentWhenUpdatesApplied() {
        Task task = taskWith(Map.of("appliedToA", 3, "appliedToB", 2));
        TaskResult result = worker.execute(task);
        assertEquals("CONSISTENT", result.getOutputData().get("status"));
    }

    @Test
    void noChangesWhenNothingApplied() {
        Task task = taskWith(Map.of("appliedToA", 0, "appliedToB", 0));
        TaskResult result = worker.execute(task);
        assertEquals("NO_CHANGES", result.getOutputData().get("status"));
    }

    @Test
    void returnsSummary() {
        Task task = taskWith(Map.of("appliedToA", 2, "appliedToB", 1));
        TaskResult result = worker.execute(task);
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("2 updates applied to A"));
        assertTrue(summary.contains("1 to B"));
    }

    @Test
    void returnsChecksumMatch() {
        Task task = taskWith(Map.of("appliedToA", 1, "appliedToB", 1));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("checksumMatch"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
