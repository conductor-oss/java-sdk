package datasync.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplyUpdatesWorkerTest {

    private final ApplyUpdatesWorker worker = new ApplyUpdatesWorker();

    @Test
    void taskDefName() {
        assertEquals("sy_apply_updates", worker.getTaskDefName());
    }

    @Test
    void appliesUpdates() {
        Task task = taskWith(Map.of(
                "toApplyA", List.of(Map.of("recordId", "R1"), Map.of("recordId", "R2")),
                "toApplyB", List.of(Map.of("recordId", "R3"))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("appliedToA"));
        assertEquals(1, result.getOutputData().get("appliedToB"));
        assertEquals(3, result.getOutputData().get("totalApplied"));
    }

    @Test
    void handlesEmptyUpdates() {
        Task task = taskWith(Map.of("toApplyA", List.of(), "toApplyB", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals(0, result.getOutputData().get("totalApplied"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
