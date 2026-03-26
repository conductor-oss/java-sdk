package gdprconsent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PresentOptionsWorkerTest {

    private final PresentOptionsWorker worker = new PresentOptionsWorker();

    @Test
    void taskDefName() {
        assertEquals("gdc_present_options", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsConsentOptions() {
        Task task = taskWith(Map.of("userId", "USR-123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<String> options = (List<String>) result.getOutputData().get("options");
        assertEquals(4, options.size());
    }

    @Test
    void includesPresentedAt() {
        Task task = taskWith(Map.of("userId", "USR-123"));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("presentedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
