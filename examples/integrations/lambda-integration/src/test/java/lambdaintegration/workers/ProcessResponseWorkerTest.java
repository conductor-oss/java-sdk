package lambdaintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessResponseWorkerTest {

    private final ProcessResponseWorker worker = new ProcessResponseWorker();

    @Test
    void taskDefName() {
        assertEquals("lam_process_response", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("statusCode", 200, "responsePayload", Map.of("body", "ok")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("success"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
