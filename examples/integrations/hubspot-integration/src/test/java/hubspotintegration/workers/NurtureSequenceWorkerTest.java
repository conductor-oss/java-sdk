package hubspotintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NurtureSequenceWorkerTest {

    private final NurtureSequenceWorker worker = new NurtureSequenceWorker();

    @Test
    void taskDefName() {
        assertEquals("hs_nurture_sequence", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("contactId", "hs-123", "ownerId", "rep-042", "segment", "mid-market"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("mid-market-welcome-series", result.getOutputData().get("sequenceName"));
        assertEquals(5, result.getOutputData().get("stepsCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
