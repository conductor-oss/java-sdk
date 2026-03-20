package incrementalrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyIndexWorkerTest {

    private final VerifyIndexWorker worker = new VerifyIndexWorker();

    @Test
    void taskDefName() {
        assertEquals("ir_verify_index", worker.getTaskDefName());
    }

    @Test
    void verifiesIndex() {
        Task task = taskWith(Map.of(
                "upsertedCount", 4,
                "docIds", List.of("doc-101", "doc-205", "doc-307", "doc-412")
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
        assertEquals(4, result.getOutputData().get("vectorCount"));
        assertEquals(12, result.getOutputData().get("queryLatencyMs"));
    }

    @Test
    void vectorCountMatchesUpsertedCount() {
        Task task = taskWith(Map.of(
                "upsertedCount", 2,
                "docIds", List.of("doc-101", "doc-205")
        ));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("vectorCount"));
        assertEquals(true, result.getOutputData().get("verified"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
