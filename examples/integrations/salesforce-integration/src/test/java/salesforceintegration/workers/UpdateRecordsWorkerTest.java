package salesforceintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdateRecordsWorkerTest {

    private final UpdateRecordsWorker worker = new UpdateRecordsWorker();

    @Test
    void taskDefName() {
        assertEquals("sfc_update_records", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("scoredLeads", List.of(Map.of("id", "l1", "score", 85))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("updatedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
