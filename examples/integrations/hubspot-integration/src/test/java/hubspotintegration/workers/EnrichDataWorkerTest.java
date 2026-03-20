package hubspotintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnrichDataWorkerTest {

    private final EnrichDataWorker worker = new EnrichDataWorker();

    @Test
    void taskDefName() {
        assertEquals("hs_enrich_data", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("contactId", "hs-123", "email", "bob@test.com", "company", "TestCorp"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SaaS", result.getOutputData().get("industry"));
        assertEquals("mid-market", result.getOutputData().get("segment"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
