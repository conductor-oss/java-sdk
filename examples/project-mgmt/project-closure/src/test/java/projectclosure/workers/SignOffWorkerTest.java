package projectclosure.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SignOffWorkerTest {
    private final SignOffWorker worker = new SignOffWorker();

    @Test void taskDefName() { assertEquals("pcl_sign_off", worker.getTaskDefName()); }

    @Test void signsOff() {
        Task task = taskWith(Map.of("projectId", "PRJ-909", "deliverables", "all complete"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("approved", result.getOutputData().get("signOff"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
