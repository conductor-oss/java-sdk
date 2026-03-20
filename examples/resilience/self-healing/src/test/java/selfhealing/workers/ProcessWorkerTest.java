package selfhealing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessWorkerTest {

    @Test
    void taskDefName() {
        ProcessWorker worker = new ProcessWorker();
        assertEquals("sh_process", worker.getTaskDefName());
    }

    @Test
    void processesDataCorrectly() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("data", "payload-1"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed-payload-1", result.getOutputData().get("result"));
    }

    @Test
    void handlesEmptyData() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("data", ""));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed-", result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingData() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed-", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
